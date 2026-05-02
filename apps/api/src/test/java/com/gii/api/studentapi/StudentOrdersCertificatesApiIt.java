package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class StudentOrdersCertificatesApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void getMyOrdersAndCertificatesReturnOnlyCurrentUserData() throws Exception {
    var student = user("Student Five", "student5@example.com");
    var other = user("Other User", "other5@example.com");
    var creator = user("Creator", "creator-stu5@example.com");
    var course = course("Course O", "course-o", creator, PublishStatus.PUBLISHED);

    var order = order(student, OrderStatus.PAID, BigDecimal.valueOf(1500));
    orderItem(order, course, BigDecimal.valueOf(1500), BigDecimal.valueOf(200));
    orderItem(
        order(other, OrderStatus.PAID, BigDecimal.valueOf(999)),
        course,
        BigDecimal.valueOf(999),
        BigDecimal.ZERO);

    certificate(student, course, "CERT-S5", false);
    certificate(other, course, "CERT-OTH5", false);

    mockMvc
        .perform(get("/student/orders").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].courseCount").value(1))
        .andExpect(jsonPath("$[0].items[0].finalAmount").value(1300));

    mockMvc
        .perform(get("/student/certificates").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].certificateCode").value("CERT-S5"))
        .andExpect(jsonPath("$[0].verificationUrl").value("/public/certificates/verify/CERT-S5"));
  }
}
