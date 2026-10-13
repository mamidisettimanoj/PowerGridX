package com.powergridx.algorithms.strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class AhoCorasick {
    static class Node {
        Map<Character, Node> children = new HashMap<>();
        Node fail;
        List<String> output = new ArrayList<>();
    }

    private Node root;

    public AhoCorasick(List<String> patterns) {
        root = new Node();
        buildTrie(patterns);
        buildFailureLinks();
    }

    private void buildTrie(List<String> patterns) {
        for (String pattern : patterns) {
            Node current = root;
            for (char c : pattern.toCharArray()) {
                current.children.putIfAbsent(c, new Node());
                current = current.children.get(c);
            }
            current.output.add(pattern);
        }
    }

    private void buildFailureLinks() {
        Queue<Node> queue = new LinkedList<>();
        root.fail = root;

        for (Node child : root.children.values()) {
            child.fail = root;
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            for (Map.Entry<Character, Node> entry : current.children.entrySet()) {
                char c = entry.getKey();
                Node child = entry.getValue();

                Node failNode = current.fail;
                while (failNode != root && !failNode.children.containsKey(c)) {
                    failNode = failNode.fail;
                }

                if (failNode.children.containsKey(c)) {
                    child.fail = failNode.children.get(c);
                } else {
                    child.fail = root;
                }

                child.output.addAll(child.fail.output);
                queue.add(child);
            }
        }
    }

    public static class Match {
        public String pattern;
        public int position;
        public Match(String pattern, int position) {
            this.pattern = pattern;
            this.position = position;
        }
    }

    public List<Match> search(String text) {
        List<Match> matches = new ArrayList<>();
        Node current = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (current != root && !current.children.containsKey(c)) {
                current = current.fail;
            }

            if (current.children.containsKey(c)) {
                current = current.children.get(c);
            } else {
                current = root;
            }

            for (String pattern : current.output) {
                matches.add(new Match(pattern, i - pattern.length() + 1));
            }
        }

        return matches;
    }
}
