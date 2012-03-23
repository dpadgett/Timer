package org.dpadgett.widget;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Implements an LRU cache with fixed capacity.
 * 
 * @author dpadgett
 */
public class LRUCache<K, V> {
	public interface Loader<K, V> {
		V load(K key, V unusedValue);
	}
	
	private static final class Node<K, V> {
		Node<K, V> parent;
		Node<K, V> child;
		K key;
		V value;
		
		Node(Node<K, V> parent, Node<K, V> child, K key, V value) {
			this.parent = parent;
			this.child = child;
			this.key = key;
			this.value = value;
		}
	}
	
	private final Loader<K, V> loader;
	private final Map<K, Node<K, V>> nodes;
	private Node<K, V> root;
	private Node<K, V> leaf;
	private final Queue<V> unusedValues;
	private final int capacity;
	
	public LRUCache(Loader<K, V> loader, int capacity) {
		this.loader = loader;
		this.capacity = capacity;
		nodes = new HashMap<K, Node<K, V>>(capacity);
		root = null;
		leaf = null;
		unusedValues = new LinkedList<V>();
	}
	
	/**
	 * Clears this LRUCache.  Prefer using this to recreating since this lets us reuse
	 * old values to make new ones.
	 */
	public void clear() {
		for (Node<K, V> node = leaf; node != null; node = node.parent) {
			unusedValues.add(node.value);
		}
		nodes.clear();
		root = null;
		leaf = null;
	}
	
	public V get(K key) {
		if (nodes.containsKey(key)) {
			// it's already in the map, so just ensure it's at root
			touch(nodes.get(key));
			return nodes.get(key).value;
		}
		// we don't have, so load value using loader
		// first, see if this load will push out an existing item
		if (nodes.size() >= capacity) {
			// push out the least recently used item (leaf)
			nodes.remove(leaf.key);
			unusedValues.add(leaf.value);
			remove(leaf);
		}
		Node<K, V> node = new Node<K, V>(null, null, key, loader.load(key, unusedValues.poll()));
		nodes.put(key, node);
		touch(node);
		return node.value;
	}

	/** Removes the given node from the list. */
	private void remove(Node<K, V> node) {
		Node<K, V> parent = node.parent;
		Node<K, V> child = node.child;
		
		if (parent != null) {
			parent.child = child;
			if (child == null) {
				leaf = parent;
			}
		}
		
		if (child != null) {
			child.parent = parent;
		}
	}

	/** Moves the given node to the root. */
	private void touch(Node<K, V> node) {
		if (root == node) {
			return;
		}

		remove(node);
		
		node.child = root;
		node.parent = null;
		if (root == null) {
			leaf = node;
		} else {
			root.parent = node;
		}
		root = node;
	}
}
