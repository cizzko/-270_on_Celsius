package core;

import core.assets.AssetsManager;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Load {
    // Название ресурса. Пустая строка означает, что название будет каждый раз запрошено из метода с сигнатурой `String $fieldName$Name()`
    String value() default "";

    // Устанавливает владение ресурсом. Если false, то жизненный цикл данного ресурса не будет отслеживаться
    boolean owned() default true;

    // Параметр загрузки ресурса
    AssetsManager.LoadType load() default AssetsManager.LoadType.ASYNC;
}
