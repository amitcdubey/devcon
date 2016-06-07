package com.devonfw.devcon.common.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Devcon command modules
 *
 * @author pparrado
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CmdModuleRegistry {
  /**
   * The name of the module
   *
   * @return name
   */
  String name() default "";

  /**
   * Description of the module
   *
   * @return description
   */
  String description() default "";

  /**
   * Context of the module
   *
   * @return context
   */
  String context() default "";

  /**
   * State of the module
   *
   * @return state
   */
  boolean deprecated() default false;
}
