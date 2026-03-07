package com.cognizant.lms.userservice.dao;

import com.cognizant.lms.userservice.config.DynamoDBConfig;
import com.cognizant.lms.userservice.domain.SkillLookups;
import com.cognizant.lms.userservice.dto.SkillCategoryResponse;
import com.cognizant.lms.userservice.dto.SkillLookupResponse;
import com.cognizant.lms.userservice.dto.TenantDTO;
import com.cognizant.lms.userservice.exception.DataBaseException;
import com.cognizant.lms.userservice.utils.TenantUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SkillLookupsDaoImplTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    @InjectMocks
    private SkillLookupsDaoImpl skillLookupsDao;

    private final String tableName = "SkillLookupsTable";

    @BeforeEach
    public void setUp() {
        when(dynamoDBConfig.dynamoDbClient()).thenReturn(dynamoDbClient);
        skillLookupsDao = new SkillLookupsDaoImpl(dynamoDBConfig, tableName);
    }

    @Test
    void getSkillCategory_withValidSkillNames_returnsSkillCategoryResponses() {
        String skillNames = "java,python";
        Map<String, AttributeValue> item1 = Map.of(
            "pk", AttributeValue.builder().s("Skill#1").build(),
            "name", AttributeValue.builder().s("java").build()
        );
        Map<String, AttributeValue> item2 = Map.of(
            "pk", AttributeValue.builder().s("Skill#2").build(),
            "name", AttributeValue.builder().s("python").build()
        );
        QueryResponse queryResponse1 = QueryResponse.builder().items(List.of(item1)).build();
        QueryResponse queryResponse2 = QueryResponse.builder().items(List.of(item2)).build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(queryResponse1)
            .thenReturn(queryResponse2);

        List<SkillCategoryResponse> result = skillLookupsDao.getSkillCategory(skillNames);

        assertEquals(2, result.size());
        assertEquals("java", result.get(0).getSkillName());
        assertEquals("python", result.get(1).getSkillName());
    }

    @Test
    void getSkillCategory_withNonExistentSkillNames_returnsEmptyList() {
        String skillNames = "nonExistentSkill";
        QueryResponse queryResponse = QueryResponse.builder().items(List.of()).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        List<SkillCategoryResponse> result = skillLookupsDao.getSkillCategory(skillNames);

        assertTrue(result.isEmpty());
    }

    @Test
    void uploadSkills_successfulBatchWrite() {
        SkillLookups skill = new SkillLookups();
        skill.setPk("pk1");
        skill.setSk("sk1");
        skill.setType("type1");
        skill.setName("name1");
        skill.setActive("true");
        skill.setGsiTypeSk("gsi1");
        skill.setEffectiveDate("2023-10-10");
        skill.setSkillCode("code1");
        skill.setSkillName("skillName1");
        skill.setSkillDescription("desc1");
        skill.setSkillType("typeA");
        skill.setStatus("active");
        skill.setSkillCategory("cat1");
        skill.setSkillSubCategory("subcat1");

        List<SkillLookups> skills = List.of(skill);
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk("t-2");
        TenantUtil.setTenantDetails(tenantDTO);

        BatchWriteItemResponse batchWriteItemResponse = BatchWriteItemResponse.builder()
                .unprocessedItems(new HashMap<>())
                .build();

        when(dynamoDbClient.batchWriteItem(any(Consumer.class))).thenReturn(batchWriteItemResponse);

        assertDoesNotThrow(() -> skillLookupsDao.uploadSkills(skills));
    }

    @Test
    void uploadSkills_batchWriteThrowsException() {
        SkillLookups skill = new SkillLookups();
        skill.setPk("pk1");
        skill.setSk("sk1");
        skill.setType("type1");
        skill.setName("name1");
        skill.setActive("true");
        skill.setGsiTypeSk("gsi1");
        skill.setEffectiveDate("2023-10-10");
        skill.setSkillCode("code1");
        skill.setSkillName("skillName1");
        skill.setSkillDescription("desc1");
        skill.setSkillType("typeA");
        skill.setStatus("active");
        skill.setSkillCategory("cat1");
        skill.setSkillSubCategory("subcat1");

        List<SkillLookups> skills = List.of(skill);
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk("t-2");
        TenantUtil.setTenantDetails(tenantDTO);

        when(dynamoDbClient.batchWriteItem(any(Consumer.class)))
                .thenThrow(DynamoDbException.builder().message("Batch write error").build());

        assertThrows(DataBaseException.class, () -> skillLookupsDao.uploadSkills(skills));
    }

    @Test
    void getSkillsAndLookupsByNameOrCode_withSkillType_combinesNameAndCode() {
        String type = "Skill";
        String search = "java";
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk("t-2");
        TenantUtil.setTenantDetails(tenantDTO);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s("t-2#skill#1").build());
        item.put("sk", AttributeValue.builder().s("sk1").build());
        item.put("name", AttributeValue.builder().s("Java").build());
        item.put("type", AttributeValue.builder().s("Skill").build());

        QueryResponse nameResponse = QueryResponse.builder().items(List.of(item)).build();
        QueryResponse codeResponse = QueryResponse.builder().items(List.of(item)).build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenAnswer(invocation -> {
            QueryRequest request = invocation.getArgument(0);
            if ("gsi_skillName".equals(request.indexName())) {
                return nameResponse;
            }
            if ("gsi_skillCode".equals(request.indexName())) {
                return codeResponse;
            }
            return QueryResponse.builder().items(List.of()).build();
        });

        SkillLookupResponse response = skillLookupsDao.getSkillsAndLookupsByNameOrCode(type, search);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getSkills());
        assertEquals(1, response.getSkills().size());
    }

    @Test
    void getSkillsAndLookupsByNameOrCode_withNonSkillType_usesSkillNameIndex() {
        String type = "Skill-Cat";
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk("t-2");
        TenantUtil.setTenantDetails(tenantDTO);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s("t-2#skill-cat#1").build());
        item.put("sk", AttributeValue.builder().s("sk1").build());
        item.put("name", AttributeValue.builder().s("Category").build());
        item.put("type", AttributeValue.builder().s("Skill-Cat").build());

        QueryResponse responsePage = QueryResponse.builder()
            .items(List.of(item))
            .lastEvaluatedKey(Collections.emptyMap())
            .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(responsePage);

        SkillLookupResponse response = skillLookupsDao.getSkillsAndLookupsByNameOrCode(type, null);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getSkills());
        assertEquals(1, response.getSkills().size());
    }

    @Test
    void getSkillsAndLookupsByNameOrCode_withSkillTypeAndEmptySearch_returnsNoContent() {
        String type = "Skill";
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setPk("t-2");
        TenantUtil.setTenantDetails(tenantDTO);

        SkillLookupResponse response = skillLookupsDao.getSkillsAndLookupsByNameOrCode(type, "");

        assertNotNull(response);
        assertEquals(204, response.getStatus());
        assertNull(response.getSkills());
    }
}


