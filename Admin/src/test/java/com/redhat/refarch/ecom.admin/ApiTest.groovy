package com.redhat.refarch.ecom.admin

import com.redhat.refarch.ecom.Application
import com.redhat.refarch.ecom.service.AdminService
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

/***
 * @author jary@redhat.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = Application.class)
class ApiTest {

    @Autowired
    AdminService adminService

    @Test
    void testApi() {
        adminService.testApi()
    }

    @Test
    void reset() {
        adminService.resetData()
    }
}
