package com.ua.wozzya.index.single;

import com.ua.wozzya.utils.Pair;
import com.ua.wozzya.extractor.ListExtractor;
import com.ua.wozzya.extractor.ReusableFileLineIterator;
import com.ua.wozzya.index.AbstractIndex;
import com.ua.wozzya.index.Index;
import com.ua.wozzya.tokenizer.Tokenizer;

import java.util.*;

/**
 * Implementation of an single
 * threaded, single-pass inverted index
 * that processes data in-memory
 */
public class InMemoryInvertedIndexStandalone extends AbstractIndex implements Index {

    private boolean readyMarker;
    private static final Pair<Long, Set<String>> EMPTY_PAIR = new Pair<>(0L, Collections.emptySet());
    private Map<String, Pair<Long, Set<String>>> index;


    protected InMemoryInvertedIndexStandalone(ListExtractor<String> listExtractor, Tokenizer tokenizer, ReusableFileLineIterator fileReader, boolean autobuild) {
        super(listExtractor, tokenizer, fileReader);

        if (autobuild) {
            buildIndex();
        }
    }

    @Override
    protected void initIndex() {
        index = new HashMap<>();
    }

    @Override
    public boolean isReady() {
        return readyMarker;
    }

    @Override
    public void buildIndex() {
        if (readyMarker) {
            throw new IllegalStateException("build method cannot be invoked twice");
        }

        initIndex();

        //extract all fileNames
        List<String> fileNames = listExtractor.extract();

        for (String fileName : fileNames) {
            collectFromFileAndStore(fileName);
        }

        readyMarker = true;
    }

    private void collectFromFileAndStore(String fileName) {
        lineIterator.setPathToFile(fileName);
        String[] tokens = tokenizer.tokenize(lineIterator.extract());
        store(tokens,fileName);
    }

    private void store(String[] tokens, String fileName) {
        for (String token : tokens) {
            Pair<Long, Set<String>> pair = index.getOrDefault(token, new Pair<>(0L, new TreeSet<>()));

            pair.setLeft(pair.getLeft() + 1);
            Set<String> curSet = pair.getRight();
            curSet.add(fileName);
            index.put(token, pair);
        }
    }

    @Override
    public Set<String> search(String key) {
        indexReadinessCheck();
        return getPair(key).getRight();
    }

    @Override
    public long getFrequency(String key) {
        indexReadinessCheck();
        return getPair(key).getLeft();
    }

    private Pair<Long, Set<String>> getPair(String key) {
        Objects.requireNonNull(key, "key shouldn't be null");
        key = key.toLowerCase();

        return index.getOrDefault(key, EMPTY_PAIR);
    }
}
