package org.greencubes.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Util {
	
	private static final Random random = new Random();
	
	public static void close(Closeable... r) {
		for(int i = 0; i < r.length; ++i)
			try {
				if(r[i] != null)
					r[i].close();
			} catch(Exception e) {
			}
	}
	
	/**
	 * <p>Performs a deep toString of provided object. It shows
	 * content of arrays and collections. Maps are not supported yet.</p>
	 * <p><b>Highly ineffective, use only for debug.</b></p>
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String toString(Object object) {
		if(object == null)
			return "null";
		StringBuilder buf = new StringBuilder();
		Class<?> eClass = object.getClass();
		
		if(eClass.isArray()) {
			if(eClass == byte[].class)
				buf.append(Arrays.toString((byte[]) object));
			else if(eClass == short[].class)
				buf.append(Arrays.toString((short[]) object));
			else if(eClass == int[].class)
				buf.append(Arrays.toString((int[]) object));
			else if(eClass == long[].class)
				buf.append(Arrays.toString((long[]) object));
			else if(eClass == char[].class)
				buf.append(Arrays.toString((char[]) object));
			else if(eClass == float[].class)
				buf.append(Arrays.toString((float[]) object));
			else if(eClass == double[].class)
				buf.append(Arrays.toString((double[]) object));
			else if(eClass == boolean[].class)
				buf.append(Arrays.toString((boolean[]) object));
			else
				// element is an array of object references
				deepToString((Object[]) object, buf, new HashSet<Object>());
		} else { // element is non-null and not an array
			if(object instanceof Collection)
				deepToString((Collection<Object>) object, buf, new HashSet<Object>());
			else
				buf.append(object.toString());
		}
		return buf.toString();
	}
	
	private static void deepToString(Collection<Object> list, StringBuilder buf, Set<Object> dejaVu) {
		Object[] array = list.toArray();
		deepToString(array, buf, dejaVu);
	}
	
	@SuppressWarnings("unchecked")
	private static void deepToString(Object[] a, StringBuilder buf, Set<Object> dejaVu) {
		if(a == null) {
			buf.append("null");
			return;
		}
		int iMax = a.length - 1;
		if(iMax == -1) {
			buf.append("[]");
			return;
		}
		
		dejaVu.add(a);
		buf.append('[');
		for(int i = 0;; i++) {
			Object element = a[i];
			if(element == null) {
				buf.append("null");
			} else {
				Class<?> eClass = element.getClass();
				
				if(eClass.isArray()) {
					if(eClass == byte[].class)
						buf.append(Arrays.toString((byte[]) element));
					else if(eClass == short[].class)
						buf.append(Arrays.toString((short[]) element));
					else if(eClass == int[].class)
						buf.append(Arrays.toString((int[]) element));
					else if(eClass == long[].class)
						buf.append(Arrays.toString((long[]) element));
					else if(eClass == char[].class)
						buf.append(Arrays.toString((char[]) element));
					else if(eClass == float[].class)
						buf.append(Arrays.toString((float[]) element));
					else if(eClass == double[].class)
						buf.append(Arrays.toString((double[]) element));
					else if(eClass == boolean[].class)
						buf.append(Arrays.toString((boolean[]) element));
					else { // element is an array of object references
						if(dejaVu.contains(element))
							buf.append("[...]");
						else
							deepToString((Object[]) element, buf, dejaVu);
					}
				} else { // element is non-null and not an array
					if(element instanceof Collection)
						deepToString((Collection<Object>) element, buf, dejaVu);
					else
						buf.append(element.toString());
				}
			}
			if(i == iMax)
				break;
			buf.append(',');
		}
		buf.append(']');
		dejaVu.remove(a);
	}
	
	/**
	 * Performs touch operations: opens file for writing and
	 * closing it modifying last edited time.
	 * @param file to perform touch operation
	 * @throws IOException if file is not writeable
	 * @author thnks for idea to Apache Commons IO
	 */
	public static void canReplaceFile(File file) throws IOException {
		if(!file.exists()) {
			FileOutputStream out = new FileOutputStream(file);
			close(out);
		}
		File f = getUniqueTmpFile(file.getParentFile());
		if(!file.renameTo(f)) {
			throw new IOException("Can not rename file");
		} else {
			f.renameTo(file);
		}
	}
	
	public static void sleepForReal(long time) throws InterruptedException {
		long end = System.currentTimeMillis() + time;
		while(true) {
			long now = System.currentTimeMillis();
			if(now >= end)
				return;
			Thread.sleep(end - now);
		}
	}
	
	private static File getUniqueTmpFile(File dir) {
		File f = null;
		do {
			long l = random.nextLong();
			if(l == Long.MIN_VALUE) {
				l = 0;
			} else {
				l = Math.abs(l);
			}
			f = new File(dir, "tmp-" + l + ".tmp");
		} while(f.exists());
		return f;
	}
}
