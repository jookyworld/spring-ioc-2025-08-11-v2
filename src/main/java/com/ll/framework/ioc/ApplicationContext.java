package com.ll.framework.ioc;

import com.ll.domain.testPost.testPost.repository.TestPostRepository;
import com.ll.domain.testPost.testPost.service.TestFacadePostService;
import com.ll.domain.testPost.testPost.service.TestPostService;

import java.util.HashMap;
import java.util.Map;

public class ApplicationContext {
    private String basePackage;
    private Map<String, Object> beans;

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
        beans = new HashMap<>();
    }

    public void init() {
    }

    public <T> T genBean(String beanName) {
        Object bean = beans.get(beanName);
        if (bean == null) {
            bean = switch (beanName) {
                case "testFacadePostService" -> new TestFacadePostService(
                        genBean("testPostService"),
                        genBean("testPostRepository")
                );
                case "testPostService" -> new TestPostService(
                        genBean("testPostRepository")
                );
                case "testPostRepository" -> new TestPostRepository();
                default -> null;
            };
            beans.put(beanName, bean);
        }

        return (T) bean;
    }
}
