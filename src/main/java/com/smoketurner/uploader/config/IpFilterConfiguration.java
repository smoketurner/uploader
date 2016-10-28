package com.smoketurner.uploader.config;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IpFilterConfiguration {

    @NotNull
    private List<String> accept = Collections.emptyList();

    @NotNull
    private List<String> reject = Collections.emptyList();

    @JsonProperty
    public List<String> getAccept() {
        return accept;
    }

    @JsonProperty
    public void setAccept(List<String> ips) {
        if (ips == null) {
            this.accept = Collections.emptyList();
        } else {
            this.accept = ips;
        }
    }

    @JsonProperty
    public List<String> getReject() {
        return reject;
    }

    @JsonProperty
    public void setReject(List<String> ips) {
        if (ips == null) {
            this.reject = Collections.emptyList();
        } else {
            this.reject = ips;
        }
    }

    @JsonIgnore
    public int count() {
        return accept.size() + reject.size();
    }
}
