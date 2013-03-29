
package org.fcrepo.utils;

import ch.lambdaj.function.convert.Converter;

import com.google.common.base.Function;

public abstract class Funktion<F, T> implements Function<F, T>, Converter<F, T> {

    @Override
    public T convert(F from) {
        // TODO Auto-generated method stub
        return apply(from);
    }
}
