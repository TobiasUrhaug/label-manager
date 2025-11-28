package org.omt.LabelManager.label;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LabelService {

    private final List<String> labels = new ArrayList<>();

    public List<String> getAllLabels() {
        return labels;
    }

    public void createLabel(String label) {
        labels.add(label);
    }
}
