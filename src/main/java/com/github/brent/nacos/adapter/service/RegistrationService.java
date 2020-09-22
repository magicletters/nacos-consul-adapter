/**
 * The MIT License
 * Copyright (c) 2019 Brent
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.brent.nacos.adapter.service;

import com.github.brent.nacos.adapter.data.ChangeItem;
import com.github.brent.nacos.adapter.data.Service;
import com.github.brent.nacos.adapter.data.ServiceHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import rx.Single;

import java.util.*;
import java.util.function.Supplier;

/**
 * Returns Services and List of Service with its last changed
 */
@Component
public class RegistrationService {

    private static final String[] NO_SERVICE_TAGS = new String[0];

    @Autowired
    private DiscoveryClient discoveryClient;

    public Single<ChangeItem<Map<String, String[]>>> getServiceNames(long waitMillis, Long index) {
        return returnDeferred(waitMillis, index, () -> {
            List<String> services = discoveryClient.getServices();
			Set<String> set = new HashSet<>(services);

            Map<String, String[]> result = new HashMap<>();
            for (String item : set) {
                result.put(item, NO_SERVICE_TAGS);
            }
            return result;
        });
    }

    public Single<ChangeItem<List<Service>>> getService(String appName, long waitMillis, Long index) {
        return returnDeferred(waitMillis, index, () -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(appName);
            List<Service> list = new ArrayList<>();

            if (instances == null) {
                return Collections.emptyList();
            } else {
                Set<ServiceInstance> instSet = new HashSet<>(instances);
                for (ServiceInstance instance : instSet) {
                    Map<String, String> metaJo = new HashMap<>();
                    metaJo.put("management.port", "" + instance.getPort());

                    Service ipObj = Service.builder()
                                           .address(instance.getHost())
                                           .node(instance.getServiceId())
                                           .serviceAddress(instance.getHost())
                                           .serviceName(instance.getServiceId())
                                           .serviceID(instance.getHost() + ":" + instance.getPort())
                                           .servicePort(instance.getPort())
                                           .nodeMeta(Collections.emptyMap())
                                           .serviceMeta(metaJo)
                                           .serviceTags(Collections.emptyList())
                                           .build();


                    list.add(ipObj);
                }
                return list;
            }
        });
    }

    public Single<ChangeItem<List<ServiceHealth>>> getServiceHealth(String appName, long waitMillis, Long index) {
        return returnDeferred(waitMillis, index, () -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(appName);
            List<ServiceHealth> list = new ArrayList<>();

            if (instances == null) {
                return Collections.emptyList();
            } else {
                Set<ServiceInstance> instSet = new HashSet<>(instances);
                for (ServiceInstance instance : instSet) {
                    ServiceHealth.Node node = ServiceHealth.Node.builder()
                                                                .name(instance.getServiceId())
                                                                .address(instance.getHost())
                                                                .meta(Collections.emptyMap())
                                                                .build();
                    ServiceHealth.Service service = ServiceHealth.Service.builder()
                                                                         .id(instance.getServiceId())
                                                                         .name(instance.getServiceId())
                                                                         .tags(Arrays.asList(NO_SERVICE_TAGS))
                                                                         .address(instance.getHost())
                                                                         .meta(Collections.emptyMap())
                                                                         .port(instance.getPort())
                                                                         .build();
                    ServiceHealth.Check check = ServiceHealth.Check.builder()
                                                                   .node(node.getName())
                                                                   .checkID("service:" + service.getId())
                                                                   .name("Service '" + service.getName() + "' check")
                                                                   .status("passing")
                                                                   .build();
                    ServiceHealth ipObj = ServiceHealth.builder()
                                                       .node(node)
                                                       .service(service)
                                                       .checks(Collections.singletonList(check))
                                                       .build();

                    list.add(ipObj);
                }
                return list;
            }
        });
    }

    private <T> Single<ChangeItem<T>> returnDeferred(long waitMillis, Long index, Supplier<T> fn) {
        return Single.just(new ChangeItem<>(fn.get(), new Date().getTime()));
    }
}
