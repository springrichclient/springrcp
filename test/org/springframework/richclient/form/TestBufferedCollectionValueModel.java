/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.richclient.form;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.springframework.beans.BeanUtils;
import org.springframework.richclient.forms.BufferedCollectionValueModel;
import org.springframework.richclient.list.ListListModel;
import org.springframework.rules.values.ValueHolder;
import org.springframework.rules.values.ValueModel;

/**
 * Test cases for {@link BufferedCollectionValueModel}
 * 
 * TODO: add listener tests
 * 
 * @author oliverh
 */
public class TestBufferedCollectionValueModel extends TestCase {

    private Class[] supportedIterfaces = new Class[] { Collection.class,
            List.class, Set.class, SortedSet.class, };

    private Class[] supportedClasses = new Class[] { ArrayList.class,
            HashSet.class, TreeSet.class, };

    public void testCreating() {
        try {
            getBufferedCollectionValueModel(null, null);
            fail("NULL wrappedType should not be supported");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        try {
            getBufferedCollectionValueModel(null, Object.class);
            fail("wrappedType can only be an instance Collection or an array");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        try {
            getBufferedCollectionValueModel(null, int[].class);
            fail("wrappedType can not be a primative array");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        for (int i = 0; i < supportedIterfaces.length; i++) {
            getBufferedCollectionValueModel(null, supportedIterfaces[i]);
        }
        try {
            getBufferedCollectionValueModel(null,
                    CustomCollectionInterface.class);
            fail("if wrappedType is an interface it must one of the standard JDK Collection interfaces");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        getBufferedCollectionValueModel(null, Object[].class);
        getBufferedCollectionValueModel(null, CustomCollectionClass.class);
        for (int i = 0; i < supportedClasses.length; i++) {
            getBufferedCollectionValueModel(null, supportedClasses[i]);
        }
    }

    public void testGetAfterBackingObjectChange() {
        Object[] backingArray = getArray(1);
        BufferedCollectionValueModel vm = getBufferedCollectionValueModel(backingArray);
        assertHasSameStructure((ListListModel)vm.get(), backingArray);

        backingArray = getArray(2);
        vm.getWrappedModel().set(backingArray);
        assertHasSameStructure((ListListModel)vm.get(), backingArray);

        vm.getWrappedModel().set(null);
        assertEquals(
                "ListListModel must have no elements when backing collection is NULL",
                ((ListListModel)vm.get()).size(), 0);

        for (int i = 0; i < supportedClasses.length; i++) {
            Collection backingCollection = getCollection(supportedClasses[i], i);
            vm = getBufferedCollectionValueModel(backingCollection);
            assertHasSameStructure((ListListModel)vm.get(), backingCollection);

            backingCollection = getCollection(supportedClasses[i], i + 1);
            vm.getWrappedModel().set(backingCollection);
            assertHasSameStructure((ListListModel)vm.get(), backingCollection);

            vm.getWrappedModel().set(null);
            assertEquals(
                    "ListListModel must have no elements when backing collection is NULL",
                    ((ListListModel)vm.get()).size(), 0);
        }
    }

    public void testChangesToListListModelWithBackingArray() {
        Object[] backingArray = getArray(100);
        BufferedCollectionValueModel vm = getBufferedCollectionValueModel(backingArray);
        ListListModel llm = (ListListModel)vm.get();
        llm.clear();
        assertEquals(
                "changes to ListListModel should be not be made to backing array unless commit is called",
                vm.getWrappedModel().get(), backingArray);

        backingArray = getArray(101);
        vm.getWrappedModel().set(backingArray);
        Object newValue = new Double(1);
        llm.set(1, newValue);
        vm.commit();
        assertEquals(
                "change should have been commited back to original array as size did not change",
                backingArray[1], newValue);

        llm.add(newValue);
        vm.commit();
        Object[] newBackingArray = (Object[])vm.getWrappedModel().get();
        assertNotSame(
                "change should not have been commited back to original array",
                newBackingArray, backingArray);
        assertTrue(newBackingArray.length == backingArray.length + 1);
        assertEquals(newBackingArray[newBackingArray.length - 1], newValue);

        llm.clear();
        vm.commit();
        newBackingArray = (Object[])vm.getWrappedModel().get();
        assertEquals(newBackingArray.length, 0);

        vm.getWrappedModel().set(null);
        llm.clear();
        vm.commit();
        assertEquals(
                "if backingCollection is NULL then a commit of an empty LLM should also be NULL",
                vm.getWrappedModel().get(), null);

        llm.add(newValue);
        vm.commit();
        newBackingArray = (Object[])vm.getWrappedModel().get();
        assertEquals(newBackingArray.length, 1);
        assertEquals(newBackingArray[0], newValue);
    }

    public void testChangesToListListModelWithBackingCollection() {
        for (int i = 0; i < supportedClasses.length; i++) {
            Collection backingCollection = getCollection(supportedClasses[i],
                    200 + i);
            BufferedCollectionValueModel vm = getBufferedCollectionValueModel(backingCollection);
            ListListModel llm = (ListListModel)vm.get();
            llm.clear();
            assertEquals(
                    "changes to LLM should be not be made to backing collection unless commit is called",
                    vm.getWrappedModel().get(), backingCollection);

            backingCollection = getCollection(supportedClasses[i], 201 + i);
            vm.getWrappedModel().set(backingCollection);
            Object newValue = new Integer(-1);
            backingCollection.remove(newValue);
            int orgSize = backingCollection.size();
            llm.set(1, newValue);
            vm.commit();
            assertTrue(
                    "change should have been commited back to original array",
                    backingCollection.contains(newValue));
            assertTrue(orgSize == backingCollection.size());

            newValue = new Integer(-2);
            backingCollection.remove(newValue);
            orgSize = backingCollection.size();
            llm.add(newValue);
            vm.commit();
            Collection newBackingCollection = (Collection)vm.getWrappedModel()
                    .get();

            assertTrue(newBackingCollection.size() == orgSize + 1);
            assertEquals(newBackingCollection, backingCollection);

            llm.clear();
            vm.commit();
            assertEquals(((Collection)vm.getWrappedModel().get()).size(), 0);

            vm.getWrappedModel().set(null);
            llm.clear();
            vm.commit();
            newBackingCollection = (Collection)vm.getWrappedModel().get();
            assertEquals(
                    "if backingCollection is NULL then a commit of an empty LLM should also be NULL",
                    newBackingCollection, null);

            llm.add(newValue);
            vm.commit();
            newBackingCollection = (Collection)vm.getWrappedModel().get();
            assertTrue(supportedClasses[i]
                    .isAssignableFrom(newBackingCollection.getClass()));
            assertEquals(newBackingCollection.size(), 1);
            assertEquals(newBackingCollection.iterator().next(), newValue);
        }
    }

    public void testListListModelKeepsStuctureOfBackingObjectAfterCommit() {
        Collection backingCollection = getCollection(HashSet.class, 1);
        int origLength = backingCollection.size();
        BufferedCollectionValueModel vm = getBufferedCollectionValueModel(backingCollection);
        ListListModel llm = (ListListModel)vm.get();
        llm.add(backingCollection.iterator().next());
        assertTrue(llm.size() == origLength + 1);
        vm.commit();
        assertTrue(
                "adding a duplicate item should not change the size of a set",
                llm.size() == origLength);
        assertHasSameStructure(llm, backingCollection);

        backingCollection = getCollection(TreeSet.class, 1);
        vm = getBufferedCollectionValueModel(backingCollection);
        llm = (ListListModel)vm.get();
        Collections.reverse(llm);
        assertTrue(((Comparable)llm.get(0)).compareTo(llm.get(1)) > 0);
        vm.commit();
        assertTrue("LLM should be sorted the same way as backingCollection",
                ((Comparable)llm.get(0)).compareTo(llm.get(1)) < 0);
        assertHasSameStructure(llm, backingCollection);
    }

    public void testIncompatibleCollections() {
        try {
            BufferedCollectionValueModel vm = getBufferedCollectionValueModel(
                    new ArrayList(), Set.class);
            fail("backing object must be assignable to backingCollectionClass");
        }
        catch (IllegalArgumentException e) {
            // expected
        }        
        try {
            BufferedCollectionValueModel vm = getBufferedCollectionValueModel(
                    new Double[0], Integer[].class);
            fail("backing object must be assignable to backingCollectionClass");
        }
        catch (IllegalArgumentException e) {
            // expected
        }

    }

    private void assertHasSameStructure(ListListModel c1, Object[] c2) {
        assertEquals("collections must be the same size", c1.size(), c2.length);
        for (int i = 0; i < c2.length; i++) {
            assertEquals(
                    "collections must have the same items in the same order",
                    c1.get(i), c2[i]);
        }
    }

    private void assertHasSameStructure(ListListModel c1, Collection c2) {
        assertEquals("collections must be the same size", c1.size(), c2.size());
        for (Iterator i = c1.iterator(), j = c2.iterator(); i.hasNext();) {
            assertEquals(
                    "collections must have the same items in the same order", i
                            .next(), j.next());
        }
    }

    private Object[] getArray(long randomSeed) {
        Random random = new Random(randomSeed);
        return new Number[] { new Integer(random.nextInt()),
                new Integer(random.nextInt()), new Integer(random.nextInt()) };
    }

    private Collection getCollection(Class collectionClass, long randomSeed) {
        Collection c = (Collection)BeanUtils.instantiateClass(collectionClass);
        Random random = new Random(randomSeed);
        c.add(new Integer(random.nextInt()));
        c.add(new Integer(random.nextInt()));
        c.add(new Integer(random.nextInt()));
        return c;
    }

    private BufferedCollectionValueModel getBufferedCollectionValueModel(
            Object backingCollecton) {
        return getBufferedCollectionValueModel(backingCollecton,
                backingCollecton.getClass());
    }

    private BufferedCollectionValueModel getBufferedCollectionValueModel(
            Object backingCollecton, Class backingCollectionClass) {
        ValueModel vm = new ValueHolder(backingCollecton);
        return new BufferedCollectionValueModel(vm, backingCollectionClass);
    }

    interface CustomCollectionInterface extends Collection {

    }

    class CustomCollectionClass extends ArrayList implements
            CustomCollectionInterface {

    }
}