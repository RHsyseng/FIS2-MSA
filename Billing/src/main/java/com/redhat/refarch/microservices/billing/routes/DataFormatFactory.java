/**
 * Copyright 2005-2017 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.refarch.microservices.billing.routes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author jary@redhat.com
 */
@Component
public class DataFormatFactory {

    protected ObjectMapper mapper;

    @PostConstruct
    public void init() {

        mapper = new ObjectMapper();

        mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(
                JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC));

        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public JacksonDataFormat formatter(Class returnClass) {
        return new JacksonDataFormat(mapper, returnClass);
    }
}
