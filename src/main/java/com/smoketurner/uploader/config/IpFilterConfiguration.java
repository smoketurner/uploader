/**
 * Copyright 2016 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
