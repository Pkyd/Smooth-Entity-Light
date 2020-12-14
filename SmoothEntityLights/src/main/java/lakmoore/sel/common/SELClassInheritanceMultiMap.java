package lakmoore.sel.common;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import net.minecraft.util.ClassInheritanceMultiMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/*
 * Use ConcurrentHashMap.newKeySet() for the "values" field
 * Concurrent so it can be updated/queried safely by ChunkGenerators in multiple threads
 * Set because we don't need the same Entity in any list more than once
 */
public class SELClassInheritanceMultiMap<T> extends ClassInheritanceMultiMap<T> {
	private final Map<Class<?>, Set<T>> map = Maps.newHashMap();
	private final Class<T> baseClass;
	private final Set<T> values = ConcurrentHashMap.newKeySet();

	public SELClassInheritanceMultiMap(Class<T> baseClassIn) {
		super(baseClassIn);
		this.baseClass = baseClassIn;
		this.map.put(baseClassIn, this.values);
	}

	public boolean add(T p_add_1_) {
		boolean flag = false;

		for (Entry<Class<?>, Set<T>> entry : this.map.entrySet()) {
			if (entry.getKey().isInstance(p_add_1_)) {
				flag |= entry.getValue().add(p_add_1_);
			}
		}

		return flag;
	}

	public boolean remove(Object p_remove_1_) {
		boolean flag = false;

		for (Entry<Class<?>, Set<T>> entry : this.map.entrySet()) {
			if (entry.getKey().isInstance(p_remove_1_)) {
				Set<T> set = entry.getValue();
				flag |= set.remove(p_remove_1_);
			}
		}

		return flag;
	}

	public boolean contains(Object p_contains_1_) {
		return this.getByClass(p_contains_1_.getClass()).contains(p_contains_1_);
	}

	public <S> Collection<S> getByClass(Class<S> p_219790_1_) {
		if (!this.baseClass.isAssignableFrom(p_219790_1_)) {
			throw new IllegalArgumentException("Don't know how to search for " + p_219790_1_);
		} else {
			Set<T> set = this.map.computeIfAbsent(p_219790_1_, (p_219791_1_) -> {
				return this.values.stream().filter(p_219791_1_::isInstance).collect(Collectors.toSet());
			});
			return (Collection<S>) Collections.unmodifiableCollection(set);
		}
	}

	public Iterator<T> iterator() {
		return (Iterator<T>) (this.values.isEmpty() ? Collections.emptyIterator()
				: Iterators.unmodifiableIterator(this.values.iterator()));
	}

	public int size() {
		return this.values.size();
	}
}