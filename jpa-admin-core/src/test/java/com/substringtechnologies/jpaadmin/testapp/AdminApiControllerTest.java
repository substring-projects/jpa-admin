package com.substringtechnologies.jpaadmin.testapp;

import com.substringtechnologies.jpaadmin.service.DynamicCrudService;
import com.substringtechnologies.jpaadmin.web.AdminApiController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AdminApiControllerTest {
    @Autowired AdminApiController controller;
    @Autowired DynamicCrudService crud;
    MockMvc mvc;
    Object accountId;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        accountId = crud.createRecord("Account", new HashMap<>(Map.of("username", "api-user"))).get("id");
    }

    @Test
    void pageSizeIsCappedAndDefaultsToTheEntitySetting() throws Exception {
        mvc.perform(get("/admin/api/Account").param("size", "1000000")).andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(AdminApiController.MAX_PAGE_SIZE));
        mvc.perform(get("/admin/api/Account").param("size", "0").param("page", "-3")).andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(1));
        mvc.perform(get("/admin/api/Account")).andExpect(jsonPath("$.pageSize").value(15));
        mvc.perform(get("/admin/api/Team")).andExpect(jsonPath("$.pageSize").value(TestApplication.DEFAULT_PAGE_SIZE));
    }

    @Test
    void businessErrorsFromActionsAreShownToTheAdmin() throws Exception {
        mvc.perform(post("/admin/api/Account/actions/suspend").contentType(MediaType.APPLICATION_JSON).content("[" + accountId + "]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Accounts on a team can't be suspended"));
    }

    @Test
    void unexpectedFailuresAreServerErrorsWithoutInternalDetails() throws Exception {
        mvc.perform(post("/admin/api/Account/actions/crash").contentType(MediaType.APPLICATION_JSON).content("[" + accountId + "]"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("ACCOUNT_SECRET"))));
    }

    @Test
    void invalidRequestsAreClientErrors() throws Exception {
        mvc.perform(get("/admin/api/Account/lookup").param("searchProperty", "password")).andExpect(status().isBadRequest());
        mvc.perform(post("/admin/api/Account").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The request is not valid."));
        mvc.perform(get("/admin/api/Missing")).andExpect(status().isNotFound());
        String tooMany = IntStream.rangeClosed(1, AdminApiController.MAX_PAGE_SIZE + 1).mapToObj(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        mvc.perform(post("/admin/api/Account/batch-delete").contentType(MediaType.APPLICATION_JSON).content(tooMany))
                .andExpect(status().isBadRequest());
    }
}
