package org.omt.LabelManager.label;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabelService {

    public List<String> getAllLabels() {
        return List.of("Mock Label");
    }

}
