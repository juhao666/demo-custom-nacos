package com.juhao666.asac.annotation;
import com.juhao666.asac.config.AsAcAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AsAcAutoConfiguration.class)
public @interface EnableAsAc {
}
