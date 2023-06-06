package com.ltsllc.commons.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by Clark on 2/23/2017.
 */
public class Bag<E extends Object> implements Iterable<E>{
    private static ImprovedRandom random = new ImprovedRandom();

    private List<E> components = new ArrayList<E>();

    public Bag () {
    }
    public Iterator<E> iterator() {
        return components.iterator();
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        for (E t : this) {
            action.accept(t);
        }
    }


    public Bag(Bag bag) {
        for (Object tempElement: bag.components) {
            E element = (E) tempElement;
            add(element);
        }
    }


    public void add (E item) {
        components.add(item);
    }

    public E get() {
        int index = random.nextIndex(components.size());
        return components.remove(index);
    }

    public boolean empty () {
        return components.size() <= 0;
    }

}
