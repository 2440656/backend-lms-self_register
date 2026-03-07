package com.cognizant.lms.userservice.utils;

import com.cognizant.lms.userservice.exception.SFTPConnectionException;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.stereotype.Component;
import net.schmizz.sshj.SSHClient;

import java.net.ConnectException;
import java.net.UnknownHostException;
import net.schmizz.sshj.userauth.UserAuthException;
import java.io.*;

@Component
@Slf4j
public class SFTPUtil {


  public String checkUsernamePasswordAuthConnection(String sftpUserName, String sftpPassword,
                                                    String sftpLocation, String sftpPort, String sftpHost) throws SFTPConnectionException {
    log.info("Starting SFTP connection check for host: {}, port: {}, location: {}",
        sftpHost, sftpPort, sftpLocation);
    try (SSHClient sshClient = new SSHClient()) {
      sshClient.addHostKeyVerifier(new PromiscuousVerifier());
      sshClient.setTimeout(30000);
      log.info("Connecting to SFTP host: {} on port: {}", sftpHost, sftpPort);
      sshClient.connect(sftpHost, Integer.parseInt(sftpPort));
      log.info("Authenticating SFTP user: {}", sftpUserName);
      sshClient.authPassword(sftpUserName, sftpPassword);
      log.info("Authentication successful for user: {}", sftpUserName);
      return checkSftpLocation(sshClient, sftpLocation);
    } catch (UserAuthException e) {
      log.error("SFTP authentication failed for user: {}", sftpUserName);
      throw new SFTPConnectionException("Authentication failed: " + e.getMessage());
    } catch (ConnectException e) {
      log.error("Connection refused to host: {} on port: {}", sftpHost, sftpPort);
      throw new SFTPConnectionException("Connection refused: " + e.getMessage());
    } catch (UnknownHostException e) {
      log.error("Unknown host: {}", sftpHost);
      throw new SFTPConnectionException("Unknown host: " + e.getMessage());
    } catch (SFTPConnectionException e) {
      throw e;
    } catch (Exception e) {
      log.error("SFTP connection failed for host: {}, port: {}, location: {}. Error: {}",
          sftpHost, sftpPort,
          sftpLocation, e.getMessage());
      log.info("SFTP connection failed : {}", e.getMessage());
      throw new SFTPConnectionException("Connection Failed: " + e);
    }

  }

  private String checkSftpLocation(SSHClient sshClient, String sftpLocation) throws SFTPConnectionException {
    try (var sftpClient = sshClient.newSFTPClient()) {
      log.info("Checking if SFTP location exists: {}", sftpLocation);
      sftpClient.stat(sftpLocation);
      log.info("SFTP location exists: {}", sftpLocation);
      return "Connection Successful";
    } catch (IOException e) {
      log.error("SFTP location does not exist : {} ", sftpLocation);
      throw new SFTPConnectionException("Connection Failed: SFTP location does not exist" + e.getMessage());
    }
  }

}
