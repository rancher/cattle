package io.cattle.platform.allocator.dao.impl;

import static org.junit.Assert.*;

import io.cattle.platform.allocator.dao.impl.AllocationCandidateIterator;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class AllocationCandidateIteratorTest {

    private boolean print = false;

    @Test
    public void test() {
        List<List<Pair<String, String>>> lists = AllocationCandidateIterator.traverse(Arrays.asList("a", "b", "c"), new LinkedHashSet<String>(Arrays.asList(
                "1", "2", "3", "4")));

        if (print) {
            for (List<Pair<String, String>> pair : lists) {
                System.out.println(StringUtils.join(pair, ","));
            }
        }

        assertEquals((long) Math.pow(4, 3), lists.size());

        lists = AllocationCandidateIterator.traverse(Arrays.asList("a", "b"), new LinkedHashSet<String>(Arrays.asList("1", "2", "3")));

        if (print) {
            for (List<Pair<String, String>> pair : lists) {
                System.out.println(StringUtils.join(pair, ","));
            }
        }

        assertEquals((long) Math.pow(3, 2), lists.size());
    }

}