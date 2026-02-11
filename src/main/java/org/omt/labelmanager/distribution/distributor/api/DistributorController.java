package org.omt.labelmanager.distribution.distributor.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/labels/{labelId}/distributors")
public class DistributorController {

    private final DistributorCommandApi commandApi;

    public DistributorController(DistributorCommandApi commandApi) {
        this.commandApi = commandApi;
    }

    @PostMapping
    public String addDistributor(
            @PathVariable Long labelId,
            @ModelAttribute AddDistributorForm form
    ) {
        commandApi.createDistributor(
                labelId,
                form.getName(),
                form.getChannelType()
        );
        return "redirect:/labels/" + labelId;
    }

    @DeleteMapping("/{distributorId}")
    public String deleteDistributor(
            @PathVariable Long labelId,
            @PathVariable Long distributorId
    ) {
        commandApi.delete(distributorId);
        return "redirect:/labels/" + labelId;
    }
}
