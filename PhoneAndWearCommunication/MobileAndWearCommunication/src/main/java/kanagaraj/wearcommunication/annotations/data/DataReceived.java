package kanagaraj.wearcommunication.annotations.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import kanagaraj.wearcommunication.annotations.util.AnnotationConstants;

/**
 * Created by kanagaraj on 31/3/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataReceived {
    String path() default "";
    int callingThread() default AnnotationConstants.BACKGROUND_THREAD;
}
