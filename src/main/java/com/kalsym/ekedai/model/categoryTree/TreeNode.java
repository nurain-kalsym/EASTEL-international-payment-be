package com.kalsym.ekedai.model.categoryTree;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TreeNode {
    private String key;
    private String label;
    private Object data;
    private List<TreeNode> children;
    private TreeNode parent;

    public TreeNode() {
    }

    public TreeNode(String key, String label) {
        this.key = key;
        this.label = label;
    }
}
