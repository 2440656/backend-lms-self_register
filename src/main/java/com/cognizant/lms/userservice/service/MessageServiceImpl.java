package com.cognizant.lms.userservice.service;

import com.cognizant.lms.userservice.constants.Constants;
import com.cognizant.lms.userservice.dao.MessageDao;
import com.cognizant.lms.userservice.domain.Tenant;
import com.cognizant.lms.userservice.dto.MessageRequest;
import com.cognizant.lms.userservice.dto.MessageResponse;
import com.cognizant.lms.userservice.utils.TenantUtil;
import com.cognizant.lms.userservice.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageDao messageDao;
    private TenantUtil tenantUtil;

    @Override
    public void publishMessage(MessageRequest request) {
        String tenantCode = tenantUtil.getTenantCode();
        String category = request.getCategory();
        String sk = tenantCode + Constants.HASH + category;
        String user = UserContext.getCreatedBy();
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(Constants.USER_EXPIRY_DATE_FORMAT));

        Tenant existingTenant = messageDao.findTenantByKey(tenantCode, category);

        Tenant tenant = new Tenant();
        tenant.setPk(tenantCode);
        tenant.setSk(sk);
        tenant.setCategory(category);
        tenant.setMessage(request.getMessage());
        tenant.setMessageStatus(request.getMessageStatus());

        if (existingTenant != null) {
            log.info("Updating existing tenant message for tenant: {}", tenantCode);
            tenant.setUpdatedDate(timestamp);
            tenant.setUpdatedBy(user);
            tenant.setCreatedOn(existingTenant.getCreatedOn());
            tenant.setCreatedBy(existingTenant.getCreatedBy());
        } else {
            log.info("Creating new tenant message entry for tenant: {}", tenantCode);
            tenant.setCreatedOn(timestamp);
            tenant.setCreatedBy(user);
        }
        log.info("Tenant message details for publishing: {}", tenant);
        messageDao.saveTenant(tenant);
    }

    @Override
    public List<MessageResponse> getMessageByCategoryAndStatus(String category, Boolean status) {
        String tenantCode = tenantUtil.getTenantCode();
        List<Tenant> tenants = messageDao.findTenantByCategoryAndStatus(tenantCode, category, status);
        log.info("Tenant message details retrieved: {}", tenants);

        if (tenants == null || tenants.isEmpty()) return Collections.emptyList();

        return tenants.stream().map(tenant -> {
            MessageResponse response = new MessageResponse();
            response.setCategory(tenant.getCategory());
            response.setMessage(tenant.getMessage());
            response.setStatus(tenant.getMessageStatus());
            return response;
        }).collect(Collectors.toList());
    }
}

