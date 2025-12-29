package com.juhao666.demo.product;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
//备用类，包含所有服务组件
//@SpringBootApplication
//@EnableScheduling
//@RestController
//@RequestMapping("/api")
public class Application {

	// 注册中心地址
	private static final String REGISTRY_URL = "http://localhost:8848/api/v1";

	// 当前服务信息
	private static final String SERVICE_NAME = "product-service";
	private static final String INSTANCE_ID = "product-service-001";
	private static final int PORT = 8002;

	// 服务发现缓存
	private final Map<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();

	// 内存存储商品数据
	private final Map<Long, Product> productDatabase = new ConcurrentHashMap<>();

	// 心跳线程池
	private final ScheduledExecutorService heartbeatExecutor =
			Executors.newSingleThreadScheduledExecutor();

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		System.out.println("==========================================");
		System.out.println("商品服务启动成功！");
		System.out.println("端口: " + PORT);
		System.out.println("API文档:");
		System.out.println("  - 获取所有商品: GET /api/products");
		System.out.println("  - 根据ID获取商品: GET /api/products/{id}");
		System.out.println("  - 创建商品: POST /api/products");
		System.out.println("  - 发现用户服务: GET /api/discover/user-service");
		System.out.println("  - 获取商品详情（带用户信息）: GET /api/products/{id}/detail");
		System.out.println("==========================================");
	}

	@PostConstruct
	public void initData() {
		// 初始化测试数据
		productDatabase.put(1L, new Product(1L, "iPhone 15 Pro", 8999.00, "苹果最新旗舰手机", "电子产品", 100));
		productDatabase.put(2L, new Product(2L, "MacBook Pro 16", 18999.00, "高性能笔记本电脑", "电子产品", 50));
		productDatabase.put(3L, new Product(3L, "AirPods Pro 2", 1499.00, "主动降噪无线耳机", "电子产品", 200));
		productDatabase.put(4L, new Product(4L, "小米电视 75寸", 4999.00, "4K超高清智能电视", "家电", 30));
		productDatabase.put(5L, new Product(5L, "海尔冰箱", 3999.00, "对开门节能冰箱", "家电", 40));

		System.out.println("✅ 初始化测试数据完成，共 " + productDatabase.size() + " 个商品");
	}

	@EventListener(ApplicationReadyEvent.class)
	public void registerToRegistry() {
		System.out.println("正在注册到注册中心...");

		RestTemplate restTemplate = new RestTemplate();
		ServiceInstance instance = new ServiceInstance();
		instance.setServiceName(SERVICE_NAME);
		instance.setInstanceId(INSTANCE_ID);
		instance.setIp("localhost");
		instance.setPort(PORT);
		instance.setStatus("UP");

		try {
			Result result = restTemplate.postForObject(
					REGISTRY_URL + "/instance/register",
					instance,
					Result.class
			);

			if (result != null && result.isSuccess()) {
				System.out.println("✅ 成功注册到注册中心");
				startHeartbeatTask();
			} else {
				System.err.println("❌ 注册失败: " + (result != null ? result.getMessage() : "未知错误"));
			}
		} catch (Exception e) {
			System.err.println("❌ 注册到注册中心失败: " + e.getMessage());
		}
	}

	/**
	 * 启动心跳任务
	 */
	private void startHeartbeatTask() {
		heartbeatExecutor.scheduleAtFixedRate(() -> {
			RestTemplate restTemplate = new RestTemplate();
			try {
				restTemplate.postForObject(
						REGISTRY_URL + "/instance/heartbeat?serviceName=" + SERVICE_NAME +
								"&instanceId=" + INSTANCE_ID,
						null,
						Result.class
				);
			} catch (Exception e) {
				System.err.println("心跳发送失败: " + e.getMessage());
			}
		}, 0, 5, TimeUnit.SECONDS);
	}

	// ==================== API接口 ====================

	@GetMapping("/products")
	public Result getAllProducts() {
		List<Product> products = new ArrayList<>(productDatabase.values());
		return Result.success("获取商品列表成功", products);
	}

	@GetMapping("/products/{id}")
	public Result getProductById(@PathVariable Long id) {
		Product product = productDatabase.get(id);
		if (product == null) {
			return Result.error("商品不存在");
		}
		return Result.success("获取商品成功", product);
	}

	@PostMapping("/products")
	public Result createProduct(@RequestBody Product product) {
		Long newId = productDatabase.keySet().stream()
				.max(Long::compareTo)
				.orElse(0L) + 1;
		product.setId(newId);
		productDatabase.put(newId, product);

		return Result.success("创建商品成功", product);
	}

	@GetMapping("/discover/user-service")
	public Result discoverUserService() {
		// 从注册中心发现用户服务
		RestTemplate restTemplate = new RestTemplate();
		try {
			Result result = restTemplate.getForObject(
					REGISTRY_URL + "/instance/list?serviceName=user-service",
					Result.class
			);

			if (result != null && result.isSuccess()) {
				Map<String, Object> data = (Map<String, Object>) result.getData();
				if (data != null) {
					List<Map<String, Object>> instancesData = (List<Map<String, Object>>) data.get("instances");
					List<ServiceInstance> instances = new ArrayList<>();

					for (Map<String, Object> instanceData : instancesData) {
						ServiceInstance instance = new ServiceInstance();
						instance.setServiceName((String) instanceData.get("serviceName"));
						instance.setInstanceId((String) instanceData.get("instanceId"));
						instance.setIp((String) instanceData.get("ip"));
						if (instanceData.get("port") instanceof Integer) {
							instance.setPort((Integer) instanceData.get("port"));
						}
						instances.add(instance);
					}

					serviceCache.put("user-service", instances);
					return Result.success("发现用户服务成功", instances);
				}
			}
			return Result.error("用户服务不可用");
		} catch (Exception e) {
			return Result.error("发现用户服务失败: " + e.getMessage());
		}
	}

	/**
	 * 获取商品详情（演示服务间调用）
	 */
	@GetMapping("/products/{id}/detail")
	public Result getProductDetail(@PathVariable Long id) {
		Product product = productDatabase.get(id);
		if (product == null) {
			return Result.error("商品不存在");
		}

		// 尝试获取用户信息（模拟商品创建者）
		RestTemplate restTemplate = new RestTemplate();

		// 首先发现用户服务
		Result discoveryResult = discoverUserService();
		if (!discoveryResult.isSuccess()) {
			return Result.success("获取商品成功，但无法获取用户信息", product);
		}

		// 获取用户服务实例
		List<ServiceInstance> userInstances = (List<ServiceInstance>) discoveryResult.getData();
		if (userInstances == null || userInstances.isEmpty()) {
			return Result.success("获取商品成功，但用户服务不可用", product);
		}

		// 使用第一个用户服务实例
		ServiceInstance userInstance = userInstances.get(0);
		String userServiceUrl = "http://" + userInstance.getIp() + ":" + userInstance.getPort() + "/api";

		try {
			// 调用用户服务获取用户信息
			Result userResult = restTemplate.getForObject(
					userServiceUrl + "/users/1",
					Result.class
			);

			Map<String, Object> detail = new HashMap<>();
			detail.put("product", product);
			detail.put("userService", userInstance);

			if (userResult != null && userResult.isSuccess()) {
				detail.put("creator", userResult.getData());
			} else {
				detail.put("creator", "获取用户信息失败");
			}

			return Result.success("获取商品详情成功", detail);
		} catch (Exception e) {
			Map<String, Object> detail = new HashMap<>();
			detail.put("product", product);
			detail.put("error", "调用用户服务失败: " + e.getMessage());
			return Result.success("获取商品详情成功（用户服务调用失败）", detail);
		}
	}

	@GetMapping("/health")
	public Result health() {
		Map<String, Object> data = new HashMap<>();
		data.put("status", "UP");
		data.put("service", SERVICE_NAME);
		data.put("instanceId", INSTANCE_ID);
		data.put("productCount", productDatabase.size());
		data.put("timestamp", System.currentTimeMillis());
		return Result.success("服务健康", data);
	}

	// ==================== 数据模型 ====================

	public static class Product {
		private Long id;
		private String name;
		private Double price;
		private String description;
		private String category;
		private Integer stock;

		public Product() {}

		public Product(Long id, String name, Double price, String description, String category, Integer stock) {
			this.id = id;
			this.name = name;
			this.price = price;
			this.description = description;
			this.category = category;
			this.stock = stock;
		}

		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }

		public String getName() { return name; }
		public void setName(String name) { this.name = name; }

		public Double getPrice() { return price; }
		public void setPrice(Double price) { this.price = price; }

		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }

		public String getCategory() { return category; }
		public void setCategory(String category) { this.category = category; }

		public Integer getStock() { return stock; }
		public void setStock(Integer stock) { this.stock = stock; }
	}

	public static class ServiceInstance {
		private String serviceName;
		private String instanceId;
		private String ip;
		private int port;
		private String status;

		public String getServiceName() { return serviceName; }
		public void setServiceName(String serviceName) { this.serviceName = serviceName; }

		public String getInstanceId() { return instanceId; }
		public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

		public String getIp() { return ip; }
		public void setIp(String ip) { this.ip = ip; }

		public int getPort() { return port; }
		public void setPort(int port) { this.port = port; }

		public String getStatus() { return status; }
		public void setStatus(String status) { this.status = status; }
	}

	public static class Result {
		private boolean success;
		private String message;
		private Object data;
		private long timestamp;

		public Result(boolean success, String message, Object data) {
			this.success = success;
			this.message = message;
			this.data = data;
			this.timestamp = System.currentTimeMillis();
		}

		public static Result success(String message) {
			return new Result(true, message, null);
		}

		public static Result success(String message, Object data) {
			return new Result(true, message, data);
		}

		public static Result error(String message) {
			return new Result(false, message, null);
		}

		public boolean isSuccess() { return success; }
		public void setSuccess(boolean success) { this.success = success; }

		public String getMessage() { return message; }
		public void setMessage(String message) { this.message = message; }

		public Object getData() { return data; }
		public void setData(Object data) { this.data = data; }

		public long getTimestamp() { return timestamp; }
		public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
	}
}

