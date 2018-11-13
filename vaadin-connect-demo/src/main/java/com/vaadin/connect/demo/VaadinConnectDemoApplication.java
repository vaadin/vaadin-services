/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.connect.demo;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import com.vaadin.connect.demo.account.Account;
import com.vaadin.connect.demo.account.AccountRepository;

/**
 * Main class of the Vaadin connect demo module.
 */
@SpringBootApplication
@EnableAuthorizationServer
@EnableWebSecurity
@EnableResourceServer
public class VaadinConnectDemoApplication {

  /**
   * Main method to run the application.
   *
   * @param args
   *          arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(VaadinConnectDemoApplication.class, args);
  }

  @Bean
  CommandLineRunner init(AccountRepository accountRepository) {
    return args -> Arrays.asList("manolo", "viktor", "kirill", "anton", "tien")
        .forEach(username -> accountRepository.save(new Account(username, "abc123")));
  }
}
