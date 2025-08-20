package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;
import lombok.SneakyThrows;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private final String basePackage;
    private final Map<String, Object> beans; // singleton cache

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
        this.beans = new HashMap<>();
    }

    public void init() {
        Set<Class<?>> componentClasses = getComponentClasses(); //컴포넌트 검색
        createBeans(componentClasses);  //컴포넌트 클래스의 인스턴스 생성
        injectDependencies(componentClasses);   //의존성 주입
    }

    private Set<Class<?>> getComponentClasses() {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getTypesAnnotatedWith(Component.class);
    }

    @SneakyThrows
    private void createBeans(Set<Class<?>> componentClasses) {
        for (Class<?> clazz : componentClasses) {   //전체 클래스 돌리면서
            String beanName = Ut.str.lcfirst(clazz.getSimpleName());    //bean 이름 받고
            try {   // 기본 생성자가 있으면 인스턴스 생성 후 저장 (repository)
                Object beanInstance = clazz.getDeclaredConstructor().newInstance();
                beans.put(beanName, beanInstance);
            } catch (NoSuchMethodException e) { // 없으면 패스 (FacadeService, Service)
                continue;
            }
        }
    }

    @SneakyThrows
    private void injectDependencies(Set<Class<?>> componentClasses) {
        for (Class<?> clazz : componentClasses) {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();    // 클래스의 모든 생성자 가져오기

            for (Constructor<?> constructor : constructors) {   // 각 생성자 돌면서
                if (constructor.getParameters().length > 0) {   // 파라미터가 있으면
                    String beanName = Ut.str.lcfirst(clazz.getSimpleName());  // 파라미터 타입의 bean 이름 생성
                    Parameter[] parameters = constructor.getParameters();   // 생성자의 파라미터들 저장
                    Object[] args = new Object[parameters.length];  // 파라미터 개수만큼 args 배열 생성

                    for (int i = 0; i < parameters.length; i++) {   // 파라미터 개수만큼 돌면서
                        Class<?> paramType = parameters[i].getType();   // 파라미터 타입 가져오기
                        String paramBeanName = Ut.str.lcfirst(paramType.getSimpleName());   // 파라미터 타입의 bean 이름 생성

                        Object dependency = beans.get(paramBeanName);   // 이미 생성된 bean 에서 의존성 찾기
                        if (dependency == null) {
                            throw new RuntimeException("Bean not found for " + paramType.getName());
                        }
                        args[i] = dependency;   // 의존성 주입을 위한 args 배열에 저장
                    }

                    // 기존 인스턴스를 제거하고 의존성이 주입된 새 인스턴스로 대체
                    Object newInstance = constructor.newInstance(args); // 생성자 호출하여 새 인스턴스 생성
                    beans.put(beanName, newInstance);   // 새 인스턴스를 beans 맵에 저장
                    break; // 의존성 주입이 완료되면 다음 클래스로
                }
            }
        }
    }

    public <T> T genBean(String beanName) {
        return (T) beans.get(beanName);
    }
}