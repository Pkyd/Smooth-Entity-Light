package lakmoore.sel.common;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.util.ClassInheritanceMultiMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SELClassInheritanceMultiMap<T> extends ClassInheritanceMultiMap<T>
{
    // Forge: Use concurrent collection to allow creating chunks from multiple threads safely
    private static final Set < Class<? >> ALL_KNOWN = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Class<?>, Boolean>());
    private final Map < Class<?>, Set<T >> map = Maps. < Class<?>, Set<T >> newHashMap();
    private final Set < Class<? >> knownKeys = Sets. < Class<? >> newIdentityHashSet();
    private final Class<T> baseClass;
    private final Set<T> values = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public SELClassInheritanceMultiMap(Class<T> baseClassIn)
    {
        super(baseClassIn);
        this.baseClass = baseClassIn;
        this.knownKeys.add(baseClassIn);
        this.map.put(baseClassIn, this.values);

        for (Class<?> oclass : ALL_KNOWN)
        {
            this.createLookup(oclass);
        }
    }

    protected void createLookup(Class<?> clazz)
    {
        ALL_KNOWN.add(clazz);

        for (T t : this.values)
        {
            if (clazz.isAssignableFrom(t.getClass()))
            {
                this.addForClass(t, clazz);
            }
        }

        this.knownKeys.add(clazz);
    }

    protected Class<?> initializeClassLookup(Class<?> clazz)
    {
    	if (this.baseClass.equals(clazz)) {
    		return clazz;
    	}

    	if (this.baseClass.isAssignableFrom(clazz))
        {
            if (!this.knownKeys.contains(clazz))
            {
                this.createLookup(clazz);
            }

            return clazz;
        }
        else
        {
            throw new IllegalArgumentException("Don't know how to search for " + clazz);
        }
    }

    public boolean add(T p_add_1_)
    {
        for (Class<?> oclass : this.knownKeys)
        {
            if (oclass.isAssignableFrom(p_add_1_.getClass()))
            {
                this.addForClass(p_add_1_, oclass);
            }
        }

        return true;
    }

    private void addForClass(T value, Class<?> parentClass)
    {
        Set<T> set = this.map.get(parentClass);

        if (set == null)
        {
        	set = java.util.concurrent.ConcurrentHashMap.newKeySet();
            this.map.put(parentClass, set); // Lists.newArrayList(value));
        }
        set.add(value);
    }

    public boolean remove(Object p_remove_1_)
    {
        T t = (T)p_remove_1_;
        boolean flag = false;

        for (Class<?> oclass : this.knownKeys)
        {
            if (oclass.isAssignableFrom(t.getClass()))
            {
                Set<T> list = this.map.get(oclass);

                if (list != null && list.remove(t))
                {
                    flag = true;
                }
            }
        }

        return flag;
    }

    public boolean contains(Object p_contains_1_)
    {
        return Iterators.contains(this.getByClass(p_contains_1_.getClass()).iterator(), p_contains_1_);
    }

    public <S> Iterable<S> getByClass(final Class<S> clazz)
    {
        return new Iterable<S>()
        {
            public Iterator<S> iterator()
            {
            	Class c = SELClassInheritanceMultiMap.this.initializeClassLookup(clazz);
                Set<T> set = SELClassInheritanceMultiMap.this.map.get(c);

                if (set == null)
                {
                    return Collections.<S>emptyIterator();
                }
                else
                {
                    Iterator<T> iterator = set.iterator();
                    // No need to filter if clazz == c
                    return clazz == c ? (Iterator<S>) iterator : Iterators.filter(iterator, clazz);
                }
            }
        };
    }
    
    public Iterator<T> iterator()
    {
        return (Iterator<T>)(this.values.isEmpty() ? Collections.emptyIterator() : Iterators.unmodifiableIterator(this.values.iterator()));
    }

    public int size()
    {
        return this.values.size();
    }
}