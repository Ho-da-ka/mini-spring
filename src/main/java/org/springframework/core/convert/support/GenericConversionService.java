package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author derekyi
 * @date 2021/1/16
 */
public class GenericConversionService implements ConversionService, ConverterRegistry {

    // 存储所有注册的转换器
    private Map<ConvertiblePair, GenericConverter> converters = new HashMap<>();

    /**
     * 检查是否可以将源类型转换为目标类型。
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 如果可以转换，则返回true；否则返回false。
     */
    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        // 获取源类型到目标类型的转换器
        GenericConverter converter = getConverter(sourceType, targetType);
        // 如果找到转换器则返回true，否则返回false
        return converter != null;
    }

    /**
     * 将源对象转换为目标类型。
     *
     * @param source     源对象
     * @param targetType 目标类型
     * @param <T>        目标类型的泛型参数
     * @return 转换后的目标对象
     */
    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        // 获取源对象的类型
        Class<?> sourceType = source.getClass();
        // 获取源类型到目标类型的转换器
        GenericConverter converter = getConverter(sourceType, targetType);
        // 使用转换器进行转换并返回结果
        return (T) converter.convert(source, sourceType, targetType);
    }

    /**
     * 向转换服务中添加一个转换器。
     *
     * @param converter 要添加的转换器
     */
    @Override
    public void addConverter(Converter<?, ?> converter) {
        // 获取转换器所需的信息
        ConvertiblePair typeInfo = getRequiredTypeInfo(converter);
        // 创建转换器适配器
        ConverterAdapter converterAdapter = new ConverterAdapter(typeInfo, converter);
        // 将适配器添加到转换器映射中
        for (ConvertiblePair convertibleType : converterAdapter.getConvertibleTypes()) {
            converters.put(convertibleType, converterAdapter);
        }
    }

    /**
     * 向转换服务中添加一个转换器工厂。
     *
     * @param converterFactory 要添加的转换器工厂
     */
    @Override
    public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
        // 获取转换器工厂所需的信息
        ConvertiblePair typeInfo = getRequiredTypeInfo(converterFactory);
        // 创建转换器工厂适配器
        ConverterFactoryAdapter converterFactoryAdapter = new ConverterFactoryAdapter(typeInfo, converterFactory);
        // 将适配器添加到转换器映射中
        for (ConvertiblePair convertibleType : converterFactoryAdapter.getConvertibleTypes()) {
            converters.put(convertibleType, converterFactoryAdapter);
        }
    }

    /**
     * 向转换服务中添加一个通用转换器。
     *
     * @param converter 要添加的通用转换器
     */
    @Override
    public void addConverter(GenericConverter converter) {
        // 将通用转换器添加到转换器映射中
        for (ConvertiblePair convertibleType : converter.getConvertibleTypes()) {
            converters.put(convertibleType, converter);
        }
    }

    private ConvertiblePair getRequiredTypeInfo(Object object) {
        // 获取对象的泛型接口类型
        Type[] types = object.getClass().getGenericInterfaces();
        // 获取第一个泛型接口的参数化类型
        ParameterizedType parameterized = (ParameterizedType) types[0];
        // 获取实际类型参数
        Type[] actualTypeArguments = parameterized.getActualTypeArguments();
        // 获取源类型和目标类型
        Class sourceType = (Class) actualTypeArguments[0];
        Class targetType = (Class) actualTypeArguments[1];
        // 返回源类型和目标类型的对
        return new ConvertiblePair(sourceType, targetType);
    }

    protected GenericConverter getConverter(Class<?> sourceType, Class<?> targetType) {
        // 获取源类型的继承层次结构
        List<Class<?>> sourceCandidates = getClassHierarchy(sourceType);
        // 获取目标类型的继承层次结构
        List<Class<?>> targetCandidates = getClassHierarchy(targetType);
        // 遍历所有可能的源类型和目标类型组合
        for (Class<?> sourceCandidate : sourceCandidates) {
            for (Class<?> targetCandidate : targetCandidates) {
                // 创建源类型和目标类型的对
                ConvertiblePair convertiblePair = new ConvertiblePair(sourceCandidate, targetCandidate);
                // 获取对应的转换器
                GenericConverter converter = converters.get(convertiblePair);
                // 如果找到转换器则返回
                if (converter != null) {
                    return converter;
                }
            }
        }
        // 如果没有找到转换器则返回null
        return null;
    }

    private List<Class<?>> getClassHierarchy(Class<?> clazz) {
        // 创建一个列表存储类的继承层次结构
        List<Class<?>> hierarchy = new ArrayList<>();
        // 遍历类及其父类
        while (clazz != null) {
            // 将当前类添加到列表中
            hierarchy.add(clazz);
            // 获取父类
            clazz = clazz.getSuperclass();
        }
        // 返回继承层次结构列表
        return hierarchy;
    }

    // 内部类，用于包装Converter
    private final class ConverterAdapter implements GenericConverter {

        private final ConvertiblePair typeInfo;

        private final Converter<Object, Object> converter;

        public ConverterAdapter(ConvertiblePair typeInfo, Converter<?, ?> converter) {
            this.typeInfo = typeInfo;
            this.converter = (Converter<Object, Object>) converter;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            // 返回支持的类型对集合
            return Collections.singleton(typeInfo);
        }

        @Override
        public Object convert(Object source, Class sourceType, Class targetType) {
            // 使用转换器进行转换并返回结果
            return converter.convert(source);
        }
    }

    // 内部类，用于包装ConverterFactory
    private final class ConverterFactoryAdapter implements GenericConverter {

        private final ConvertiblePair typeInfo;

        private final ConverterFactory<Object, Object> converterFactory;

        public ConverterFactoryAdapter(ConvertiblePair typeInfo, ConverterFactory<?, ?> converterFactory) {
            this.typeInfo = typeInfo;
            this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            // 返回支持的类型对集合
            return Collections.singleton(typeInfo);
        }

        @Override
        public Object convert(Object source, Class sourceType, Class targetType) {
            // 使用转换器工厂获取转换器并进行转换
            return converterFactory.getConverter(targetType).convert(source);
        }
    }
}
