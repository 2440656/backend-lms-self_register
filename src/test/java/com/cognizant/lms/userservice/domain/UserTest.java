package com.cognizant.lms.userservice.domain;

import com.cognizant.lms.userservice.dto.TenantDTO;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserTest {

    @Test
    void testNoArgsConstructor() {
        User user = new User();
        assertNull(user.getPk());
        assertNull(user.getSk());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getEmailId());
        assertNull(user.getTenant());
        assertNull(user.getCountry()); // Verify country is null
    }

    @Test
    void testAllArgsConstructor() {
        User user = new User(
                "firstName1", "lastName1", "institutionName1", "email1@example.com",
                "userType1", "role1", "2023-12-31", "true", "loginOption1"
        );
        user.setCountry("USA"); // Set country explicitly

        assertEquals("firstName1", user.getFirstName());
        assertEquals("lastName1", user.getLastName());
        assertEquals("institutionName1", user.getInstitutionName());
        assertEquals("email1@example.com", user.getEmailId());
        assertEquals("userType1", user.getUserType());
        assertEquals("role1", user.getRole());
        assertEquals("2023-12-31", user.getUserAccountExpiryDate());
        assertEquals("true", user.getViewOnlyAssignedCourses());
        assertEquals("loginOption1", user.getLoginOption());
        assertEquals("USA", user.getCountry()); // Verify country
    }

    @Test
    void testSettersAndGetters() {
        User user = new User();
        user.setPk("pk1");
        user.setSk("sk1");
        user.setFirstName("firstName1");
        user.setLastName("lastName1");
        user.setEmailId("email1@example.com");
        user.setTenant(new TenantDTO("tenantPk1", "tenantName", "tenantName", "idpPreferences1", "portal", "clientId", "issuer", "certUrl"));
        user.setCountry("India"); // Set country

        assertEquals("pk1", user.getPk());
        assertEquals("sk1", user.getSk());
        assertEquals("firstName1", user.getFirstName());
        assertEquals("lastName1", user.getLastName());
        assertEquals("email1@example.com", user.getEmailId());
        assertEquals("tenantPk1", user.getTenant().getPk());
        assertEquals("idpPreferences1", user.getTenant().getIdpPreferences());
        assertEquals("India", user.getCountry()); // Verify country
    }

    @Test
    void testToString() {
        User user = new User(
            "firstName1", "lastName1", "institutionName1", "email1@example.com",
            "userType1", "role1", "2023-12-31", "true", "loginOption1"
        );
        user.setCountry("Canada"); // Set country

        String expected = "User(pk=null, sk=null, firstName=firstName1, lastName=lastName1, gsiSortFNLN=null, name=null, institutionName=institutionName1, userType=userType1, role=role1, status=null, userAccountExpiryDate=2023-12-31, emailId=email1@example.com, type=null, createdOn=null, modifiedOn=null, modifiedBy=null, createdBy=null, menteeId=null, mentorId=null, tenantCode=null, lastLoginTimestamp=null, viewOnlyAssignedCourses=true, loginOption=loginOption1, passwordChangedDate=null, reactivatedDate=null, portal=null, country=Canada, termsAccepted=null, termsAcceptedDate=null, preferredUI=null, isWatchedTutorial=null, tutorialWatchDate=null, videoLaunchCount=null, tenant=null)";

        assertEquals(expected, user.toString());
    }


    @Test
    void testEqualsAndHashCode() {
        User user1 = new User(
                "firstName1", "lastName1", "institutionName1", "email1@example.com",
                "userType1", "role1", "2023-12-31", "true", "loginOption1"
        );
        User user2 = new User(
                "firstName1", "lastName1", "institutionName1", "email1@example.com",
                "userType1", "role1", "2023-12-31", "true", "loginOption1"
        );

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void testDynamoDbIgnoreAnnotation() {
        User user = new User();
        user.setTenant(new TenantDTO("t-2", "tenantName", "tenantName", "SSO", "portal", "clientId", "issuer", "certUrl"));


        // Verify that the tenant field is ignored by DynamoDB
        TableSchema<User> tableSchema = StaticTableSchema.builder(User.class).build();
        assertFalse(tableSchema.attributeNames().contains("tenant"));
    }
}