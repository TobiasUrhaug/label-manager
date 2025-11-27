package org.omt.LabelManager.label;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class LabelController {

    @GetMapping("/labels")
    public String labels(Model model) {
        model.addAttribute("user", "Alex The Manager");
        return "labels";
    }

}
