/*******************************************************************************
 * Copyright 2015-2018 Capgemini SE.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/
package com.devonfw.devcon.common.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import javax.activation.DataHandler;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap60.webservices.filestorage.IFileStorageAppSoap;
import com.collabnet.ce.soap60.webservices.frs.FrsFileSoapDO;
import com.collabnet.ce.soap60.webservices.frs.IFrsAppSoap;
import com.devonfw.devcon.Devcon;
import com.devonfw.devcon.modules.dist.DistConstants;
import com.devonfw.devcon.output.ConsoleOutput;
import com.devonfw.devcon.output.DownloadingProgress;
import com.devonfw.devcon.output.Output;
import com.google.common.base.Optional;

/**
 * Class to encapsulate the functionality related to the Team Forge download process.
 *
 * @author pparrado
 */
public class Downloader {

  public static DownloadingProgress progressBar;

  /**
   * Downloads a file from Team Forge repository
   *
   * @param path the path where the file should be downloaded
   * @param user a user with permissions to download from Team Forge repository
   * @param password the user password
   * @param frsFileId the id of the distribution in Team Forge
   * @throws Exception
   */
  public static Optional<String> downloadFromTeamForge(String path, String user, String password, String frsFileId)
      throws Exception {

    Thread thread = null;
    // DownloadingProgress progressBar = null;
    Output out = new ConsoleOutput();
    String fileName = "";
    String tempFilePath = "";
    String userTempDir = System.getProperty("java.io.tmpdir");

    try {
      ICollabNetSoap _sfSoap =
          (ICollabNetSoap) ClientSoapStubFactory.getSoapStub(ICollabNetSoap.class, DistConstants.REPOSITORY_URL);

      if (_sfSoap != null) {

        String sessionId = _sfSoap.login(user, password);
        if (sessionId != null) {
          IFileStorageAppSoap _fileStorageAppSoap = (IFileStorageAppSoap) ClientSoapStubFactory
              .getSoapStub(IFileStorageAppSoap.class, DistConstants.REPOSITORY_URL);

          IFrsAppSoap frsAppSoap =
              (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, DistConstants.REPOSITORY_URL);

          FrsFileSoapDO file = frsAppSoap.getFrsFileData(sessionId, frsFileId);
          if (file != null) {

            fileName = file.getFilename();

            File fileInLocal = new File(path + File.separator + fileName);

            if (fileInLocal.exists())
              throw new FileAlreadyExistsException(path + File.separator + fileName);

            fileInLocal.getParentFile().mkdirs();

            String fileStorageId = frsAppSoap.getFrsFileId(sessionId, frsFileId);
            Double size = (file.getSize() / 1024.0) / 1024.0;
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);

            out.status(
                "Downloading " + file.getFilename() + " (" + df.format(size) + "MB). It may take a few minutes.");

            // start showing progressBar
            progressBar = new DownloadingProgress(file.getSize(), userTempDir);
            thread = new Thread(progressBar);
            thread.start();

            // downloading
            DataHandler hdl = _fileStorageAppSoap.downloadFile(sessionId, fileStorageId);

            // end progressBar
            if (thread != null) {
              progressBar.terminate();
              thread.join();
            }

            // converting the temporal file to a "real" file
            if (hdl != null) {
              tempFilePath = hdl.getName();
              InputStream is = hdl.getInputStream();
              OutputStream os = new FileOutputStream(new File(path + File.separator + fileName));
              IOUtils.copy(is, os);
              IOUtils.closeQuietly(os);
              IOUtils.closeQuietly(is);
              out.statusInNewLine("File downloaded successfully.");
            }

          } else {
            throw new FileNotFoundException();
          }

        }

      }

      return Optional.of(fileName);

    } catch (RemoteException e) {
      out.showError(e.getMessage());
      return null;
    } catch (FileNotFoundException e) {
      out.showError("Download failed. File " + fileName + " not found in the repository " + DistConstants.REPOSITORY_URL
          + ". " + e.getMessage());
      return null;
    } catch (FileAlreadyExistsException e) {
      out.showError("Download failed. File " + e.getFile() + " already exists.");
      return null;
    } catch (Exception e) {
      out.showError(e.getMessage());
      return null;
    } finally {
      File tempFile = new File(tempFilePath);
      if (tempFile.exists()) {
        // TODO implement logs
        System.out.println("[LOG] Deleting temp file " + tempFile.getPath() + "...");
        tempFile.delete();
        System.out.println("[LOG] Temp file " + tempFile.getPath() + " deleted.");
      }

    }

  }

  /**
   * Downloads a file from a URL
   *
   * @param source the location of the file
   * @param path the file destiny in local machine
   * @param tempFileName the temporary file name
   * @throws Exception if the file can not be found or the streams can't be managed
   */
  public void downloadFile(String source, String path, String tempFileName) throws Exception {

    OutputStream outputStream = null;
    InputStream inputStream = null;

    try {

      Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("1.0.5.10", 8080));

      // File ZIP without authentication [OK]------------------------------------------

      URL url = new URL(source);

      File folder = new File(path);
      if (!folder.exists()) {
        folder.mkdirs();
      }

      outputStream =
          new BufferedOutputStream(new FileOutputStream(new File(path + File.separator + tempFileName /*
                                                                              * + ".zip"
                                                                              */)));
      inputStream = url.openConnection(proxy).getInputStream();
      final byte[] buffer = new byte[65536];
      while (true) {
        final int len = inputStream.read(buffer);
        if (len < 0) {
          break;
        }
        outputStream.write(buffer, 0, len);

      }

    } catch (Exception e) {
      throw e;
    } finally {
      if (outputStream != null)
        outputStream.close();
    }

  }

  /**
   * Method to obtain the Devcon's configuration properties from the devonfw.github.io repository, from the
   * "version.json" file
   *
   * @param property the reference of the file in Teamforge
   * @return the file ID
   */
  public static Optional<String> getDevconConfigProperty(String property) {

    try {
      String propertyValue = null;
      JSONObject json = null;

      json = new JSONObject(IOUtils.toString(new URL(Devcon.VERSION_URL), Charset.forName("UTF-8")));

      propertyValue = (String) json.get(property);
      return Optional.of(propertyValue);
    } catch (Exception e) {
      return Optional.absent();
    }

  }

}
