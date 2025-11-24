package com.educode.educodeApi.lazyinit;

public interface LazyInclude<T> {
    void initialize(T entity);
}
