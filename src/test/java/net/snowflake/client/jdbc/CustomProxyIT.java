package net.snowflake.client.jdbc;

import net.snowflake.client.category.TestCategoryOthers;
import net.snowflake.common.core.SqlState;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.sql.*;
import java.util.Properties;

import static junit.framework.TestCase.*;
import static net.snowflake.client.AbstractDriverIT.getFullPathFileInResource;
import static net.snowflake.client.jdbc.SnowflakeUtil.systemGetProperty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Category(TestCategoryOthers.class)
public class CustomProxyIT {
  @Test
  //@Ignore
  public void testCorrectProxySettingFromConnectionString()
      throws ClassNotFoundException, SQLException {

    String connectionUrl =
        "jdbc:snowflake://s3testaccount.us-east-1.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=8080"
            + "&useProxy=true";
    // should finish correctly
    runProxyConnection(connectionUrl);

    /*connectionUrl =
        "jdbc:snowflake://s3testaccount.us-east-1.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=3128"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&useProxy=true";*/
    // should finish correctly
    runProxyConnection(connectionUrl);
  }

  @Test
  //@Ignore
  public void testWrongProxyPortSettingFromConnectionString()
      throws ClassNotFoundException, SQLException {

    String connectionUrl =
        "jdbc:snowflake://s3testaccount.us-east-1.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=31281"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&nonProxyHosts=*.foo.com|localhost&useProxy=true";
    // should show warning for null response for the requests
    runProxyConnection(connectionUrl);
  }

  @Test
  //@Ignore
  public void testWrongProxyPasswordSettingFromConnectionString()
      throws ClassNotFoundException, SQLException {

    String connectionUrl =
        "jdbc:snowflake://s3testaccount.us-east-1.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=3128"
            + "&proxyUser=testuser2&proxyPassword=test111"
            + "&nonProxyHosts=*.foo.com|localhost&useProxy=true";
    // should show warning for null response for the requests
    try {
      runProxyConnection(connectionUrl);
    } catch (SQLException e) {
      assertThat(
          "JDBC driver encountered communication error",
          e.getErrorCode(),
          equalTo(ErrorCode.NETWORK_ERROR.getMessageCode()));
    }
  }

  @Test
  //@Ignore
  public void testInvalidProxyPortFromConnectionString()
      throws ClassNotFoundException, SQLException {

    String connectionUrl =
        "jdbc:snowflake://s3testaccount.us-east-1.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&nonProxyHosts=*.foo.com|localhost&useProxy=true";
    // should throw SnowflakeSQLException: 200051
    try {
      runProxyConnection(connectionUrl);
    } catch (SQLException e) {
      assertThat(
          "invalid proxy error",
          e.getErrorCode(),
          equalTo(ErrorCode.INVALID_PROXY_PROPERTIES.getMessageCode()));
    }
  }

  @Test
  //@Ignore
  public void testNonProxyHostsFromConnectionString() throws ClassNotFoundException, SQLException {

    String connectionUrl =
        "jdbc:snowflake://s3testaccount.us-east-1.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=31281"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&nonProxyHosts=*.snowflakecomputing.com|localhost&useProxy=true";
    // should finish correctly
    runProxyConnection(connectionUrl);
  }

  @Test
  //@Ignore
  public void testWrongNonProxyHostsFromConnectionString()
      throws ClassNotFoundException, SQLException {

    String connectionUrl =
        "jdbc:snowflake://s3testaccount.us-east-1.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=31281"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&nonProxyHosts=*.foo.com|localhost&useProxy=true";
    // should fail to connect

    runProxyConnection(connectionUrl);
  }

  public void runProxyConnection(String connectionUrl) throws ClassNotFoundException, SQLException {
    Authenticator.setDefault(
        new Authenticator() {
          @Override
          public PasswordAuthentication getPasswordAuthentication() {
            System.out.println("RequestorType: " + getRequestorType());
            System.out.println("Protocol: " + getRequestingProtocol().toLowerCase());
            return new PasswordAuthentication(
                systemGetProperty("http.proxyUser"),
                systemGetProperty("http.proxyPassword").toCharArray());
          }
        });

    System.setProperty("http.useProxy", "true");
    System.setProperty("http.proxyHost", "localhost");
    System.setProperty("http.proxyPort", "8080");
    System.setProperty("https.proxyHost", "localhost");
    System.setProperty("https.proxyPort", "8080");

    // SET USER AND PASSWORD FIRST
    String user = "mknister";
    String passwd = "Argumentc1inicspam!";
    Properties _connectionProperties = new Properties();
    _connectionProperties.put("user", user);
    _connectionProperties.put("password", passwd);
    _connectionProperties.put("role", "accountadmin");
    _connectionProperties.put("database", "SNOWHOUSE_IMPORT");
    _connectionProperties.put("schema", "DEV");

    Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");
    long counter = 0;
    while (true) {
      Connection con = DriverManager.getConnection(connectionUrl, _connectionProperties);
      Statement stmt = con.createStatement();
      stmt.execute("use warehouse TINY_WAREHOUSE");
      stmt.execute("CREATE OR REPLACE STAGE testPutGet_stage");
      assertTrue(
              "Failed to put a file",
              stmt.execute(
                      "PUT file://" + getFullPathFileInResource("orders_100.csv") + " @testPutGet_stage"));
      /*String sql = "select $1 from values(1),(3),(5),(7)";
      ResultSet res = stmt.executeQuery(sql);
      while (res.next()) {
        System.out.println("value: " + res.getInt(1));
      }
      System.out.println("OK - " + counter);*/
      con.close();
      counter++;
      break;
    }
  }

  @Test
  //@Ignore
  public void testProxyConnectionWithAzure() throws ClassNotFoundException, SQLException {
    String connectionUrl =
        "jdbc:snowflake://aztestaccount.east-us-2.azure.snowflakecomputing.com/?tracing=ALL";
    runAzureProxyConnection(connectionUrl, true);
  }

  @Test
  //@Ignore
  public void testProxyConnectionWithAzureWithConnectionString()
      throws ClassNotFoundException, SQLException {
    String connectionUrl =
        "jdbc:snowflake://aztestaccount.east-us-2.azure.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=8080"
            + "&useProxy=true";
    ;
    runAzureProxyConnection(connectionUrl, false);
  }

  @Test
  //@Ignore
  public void testProxyConnectionWithoutProxyPortOrHost()
      throws ClassNotFoundException, SQLException {
    // proxyPort is empty
    String connectionUrl =
        "jdbc:snowflake://aztestaccount.east-us-2.azure.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort="
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&useProxy=true";
    try {
      runAzureProxyConnection(connectionUrl, false);
      fail();
    } catch (SQLException e) {
      assertEquals(SqlState.CONNECTION_EXCEPTION, e.getSQLState());
    }
    // proxyPort is non-integer value
    connectionUrl =
        "jdbc:snowflake://aztestaccount.east-us-2.azure.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=cheese"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&useProxy=true";
    try {
      runAzureProxyConnection(connectionUrl, false);
      fail();
    } catch (SQLException e) {
      assertEquals(SqlState.CONNECTION_EXCEPTION, e.getSQLState());
    }

    // proxyHost is empty, proxyPort is valid
    connectionUrl =
        "jdbc:snowflake://aztestaccount.east-us-2.azure.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=&proxyPort=3128"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&useProxy=true";
    try {
      runAzureProxyConnection(connectionUrl, false);
      fail();
    } catch (SQLException e) {
      assertEquals(SqlState.CONNECTION_EXCEPTION, e.getSQLState());
    }

    // proxyPort and proxyHost are empty, but username and password are specified
    connectionUrl =
        "jdbc:snowflake://aztestaccount.east-us-2.azure.snowflakecomputing.com/?tracing=ALL"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&useProxy=true";
    try {
      runAzureProxyConnection(connectionUrl, false);
      fail();
    } catch (SQLException e) {
      assertEquals(SqlState.CONNECTION_EXCEPTION, e.getSQLState());
    }
  }

  @Test
  //@Ignore
  public void testProxyConnectionWithAzureWithWrongConnectionString()
      throws ClassNotFoundException, SQLException {
    String connectionUrl =
        "jdbc:snowflake://aztestaccount.east-us-2.azure.snowflakecomputing.com/?tracing=ALL"
            + "&proxyHost=localhost&proxyPort=31281"
            + "&proxyUser=testuser1&proxyPassword=test"
            + "&nonProxyHosts=*.foo.com%7Clocalhost&useProxy=true";

    try {
      runAzureProxyConnection(connectionUrl, false);
    } catch (SQLException e) {
      assertThat(
          "JDBC driver encountered communication error",
          e.getErrorCode(),
          equalTo(ErrorCode.NETWORK_ERROR.getMessageCode()));
    }
  }

  public void runAzureProxyConnection(String connectionUrl, boolean usesProperties)
      throws ClassNotFoundException, SQLException {
    Authenticator.setDefault(
        new Authenticator() {
          @Override
          public PasswordAuthentication getPasswordAuthentication() {
            System.out.println("RequestorType: " + getRequestorType());
            System.out.println("Protocol: " + getRequestingProtocol().toLowerCase());
            return new PasswordAuthentication(
                systemGetProperty("http.proxyUser"),
                systemGetProperty("http.proxyPassword").toCharArray());
          }
        });

    /*System.setProperty("http.useProxy", "true");
    System.setProperty("http.proxyHost", "localhost");
    System.setProperty("http.proxyPort", "8080");
    System.setProperty("https.proxyHost", "localhost");
    System.setProperty("https.proxyPort", "8080");*/


    // SET USER AND PASSWORD FIRST
    String user = "mknister";
    String passwd = "Argumentc1inicspam!";
    Properties _connectionProperties = new Properties();
    _connectionProperties.put("user", user);
    _connectionProperties.put("password", passwd);
    _connectionProperties.put("role", "SYSADMIN");
    if (usesProperties) {
      _connectionProperties.put("useProxy", true);
      _connectionProperties.put("proxyHost", "localhost");
      _connectionProperties.put("proxyPort", "3128");
      _connectionProperties.put("proxyUser", "testuser1");
      _connectionProperties.put("proxyPassword", "test");
    }

    Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");

    String fileName = "test_copy.csv";
    URL resource = StatementIT.class.getResource(fileName);
    Connection con = DriverManager.getConnection(connectionUrl, _connectionProperties);
    Statement stmt = con.createStatement();
    stmt.execute("create or replace warehouse MEGTEST");
    stmt.execute("use database MEGDB");
    stmt.execute("use schema MEGSCHEMA");
    stmt.execute("CREATE OR REPLACE STAGE testPutGet_stage");
    assertTrue(
        "Failed to put a file",
        stmt.execute(
            "PUT file://" + getFullPathFileInResource("orders_100.csv") + " @testPutGet_stage"));
  }
}
