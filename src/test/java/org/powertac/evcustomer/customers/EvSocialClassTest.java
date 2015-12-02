/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.evcustomer.customers;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.config.ConfigurationRecorder;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.evcustomer.Config;
import org.powertac.evcustomer.ConfigTest;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.CarType;
import org.powertac.evcustomer.beans.ClassCar;
import org.powertac.evcustomer.beans.ClassGroup;
import org.powertac.evcustomer.beans.SocialGroup;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Govert Buijs, John Collins
 */
public class EvSocialClassTest
{
  private TimeslotRepo timeslotRepo;

  private CustomerRepo mockCustomerRepo;

  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed mockSeed;

  private DummyConfig serverConfiguration;
  private ServiceAccessor service;

  private String className = "HighIncome_2";

  private EvSocialClass evSocialClass;

  @Before
  public void setUp ()
  {
    evSocialClass = new EvSocialClass(className);
    mockSeedRepo = mock(RandomSeedRepo.class);
    mockSeed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyInt(),
                                    anyString())).thenReturn(mockSeed);
    mockCustomerRepo = mock(CustomerRepo.class);

    serverConfiguration = new DummyConfig();
    serverConfiguration.initialize();
    Config config = Config.getInstance();
    ReflectionTestUtils.setField(config, "serverConfiguration",
                                 serverConfiguration);

    service = new ServiceAccessor();

//    socialGroup = new SocialGroup(groupId, groupName);
//    socialGroups = new HashMap<Integer, SocialGroup>();
//    activities = new HashMap<Integer, Activity>();
//    activity = new Activity(0, "Test Activity", 1.0, 1.0);
//    groupActivities = new HashMap<Integer, GroupActivity>();
//    groupActivity = new GroupActivity(0, 10, 10, 1.0, 1.0);
//    carTypes = new ArrayList<CarType>();
//
//    customerRepo.recycle();
//    tariffSubscriptionRepo.recycle();
//    tariffRepo.recycle();
//    Broker broker1 = new Broker("Joe");
//
//    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
//    timeService.setCurrentTime(now.toInstant());
//    Instant exp = new Instant(now.getMillis() + TimeService.WEEK * 10);
//
//    defaultTariffSpec =
//        new TariffSpecification(broker1, PowerType.CONSUMPTION)
//            .withExpiration(exp).withMinDuration(TimeService.WEEK * 8)
//            .addRate(new Rate().withValue(-0.222));
//    defaultTariff = new Tariff(defaultTariffSpec);
//    defaultTariff.init();
//    defaultTariff.setState(Tariff.State.OFFERED);
//
//    defaultTariffSpecEV =
//        new TariffSpecification(broker1, PowerType.ELECTRIC_VEHICLE)
//            .withExpiration(exp).withMinDuration(TimeService.WEEK * 8)
//            .addRate(new Rate().withValue(-0.121).withMaxCurtailment(0.3));
//    defaultTariffEV = new Tariff(defaultTariffSpecEV);
//    defaultTariffEV.init();
//    defaultTariffEV.setState(Tariff.State.OFFERED);
//
//    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
//        .thenReturn(defaultTariff);
//    when(mockTariffMarket.getDefaultTariff(PowerType.ELECTRIC_VEHICLE))
//        .thenReturn(defaultTariffEV);
  }

  @After
  public void shutDown ()
  {
    Config.recycle();
  }

  private void initializeClass ()
  {
    evSocialClass.setServiceAccessor(service);
    evSocialClass.setMinCount(2);
    evSocialClass.setMaxCount(4);
    evSocialClass.initialize();
  }

  @Test
  public void testInitialization ()
  {
    initializeClass();
    assertEquals("Correct name", className, evSocialClass.getName());
    assertEquals("correct min count", 2, evSocialClass.getMinCount());
  }

  @Test
  public void testBeans ()
  {
    initializeClass();
    Map<Integer, SocialGroup> groups = evSocialClass.getGroups();
    assertEquals("3 groups", 3, groups.size());
    assertEquals("includes parttime", "parttime", groups.get(1).getName());

    Map<String, CarType> carTypes = evSocialClass.getCarTypes();
    assertEquals("2 cartypes", 2, carTypes.size());
    assertTrue("includes Tesla_40_kWh",
        carTypes.keySet().contains("Tesla_40_kWh"));
    assertTrue("includes Nissan_Leaf_24_kWh",
        carTypes.keySet().contains("Nissan_Leaf_24_kWh"));

    Map<Integer, Activity> activities = evSocialClass.getActivities();
    assertEquals("2 activities", 2, activities.size());
    assertEquals("commuting activity", "commuting",
        activities.get(0).getName());
    assertEquals("business_trip activity", "business_trip",
        activities.get(1).getName());

    Map<String, ClassCar> classCars = evSocialClass.getClassCars();
    assertEquals("2 cartypes", 2, classCars.size());
    assertTrue("includes Tesla_40_kWh",
        classCars.keySet().contains("Tesla_40_kWh"));
    assertTrue("includes Nissan_Leaf_24_kWh",
        classCars.keySet().contains("Nissan_Leaf_24_kWh"));

    Map<Integer, ClassGroup> classGroups = evSocialClass.getClassGroups();
    assertEquals("twelve class-groups", 3, classGroups.size());
    ClassGroup hi2_1 = classGroups.get(1);
    assertEquals("correct socialClassName",
                 "HighIncome_2", hi2_1.getSocialClassName());
    assertEquals("correct probability", 0.125, hi2_1.getProbability(), 1e-6);
  }

  @Test
  public void testEvCustomers ()
  {
    initializeClass();
    assertEquals("correct min count", 2, evSocialClass.getMinCount());
    assertEquals("correct max count", 4, evSocialClass.getMaxCount());
    ArrayList<EvCustomer> customers = evSocialClass.getEvCustomers();
    assertEquals("correct population", 2, evSocialClass.getPopulation());
    assertEquals("correct number of customers", 2, customers.size());
    assertEquals("correct number of infos", 2,
        evSocialClass.getCustomerInfos().size());
    assertEquals("correct name", "HighIncome_2_0",
                 customers.get(0).getName());
    assertEquals("correct boot-config list 0",
                "0.male.Tesla_40_kWh.x",
                 evSocialClass.getCustomerAttributeList().get(0));
    assertEquals("correct boot-config list 1",
                 "0.male.Tesla_40_kWh.x",
                 evSocialClass.getCustomerAttributeList().get(1));
  }

  @Test
  public void testBootConfig ()
  {
    initializeClass();
    Configurator testConfig = new Configurator();
    ConfigurationPublisher pub = new ConfigurationPublisher();
    testConfig.gatherBootstrapState(evSocialClass, pub);
    assertEquals("one property", 1, pub.getConfig().size());
    Object pop =
        pub.getConfig().get("evcustomer.customers.evSocialClass.customerAttributeList");
    @SuppressWarnings("unchecked")
    List<String> popList = (List<String>)pop;
    assertEquals("2 items", 2, popList.size());
    assertEquals("correct customer instance 0",
                "0.male.Tesla_40_kWh.x", popList.get(0));
    assertEquals("correct customer instance 1",
                "0.male.Tesla_40_kWh.x", popList.get(1));
  }

  // Boot-restore is triggered and creates the correct objects
  @Test
  public void testBootRestoreTrigger ()
  {
    ArrayList<String> gcList = new ArrayList<String>();
    gcList.add("0.male.Tesla_40_kWh.0");
    gcList.add("2.female.Nissan_Leaf_24_kWh.1");
    gcList.add("1.female.Tesla_40_kWh.2");
    ReflectionTestUtils.setField(evSocialClass, "customerAttributeList",
                                 gcList);
    initializeClass();
    assertEquals("3 instances", 3, evSocialClass.getPopulation());
    List<EvCustomer> customers = evSocialClass.getEvCustomers();
    assertEquals("3 in list", 3, customers.size());
    EvCustomer cust = customers.get(0);
    assertEquals("correct name", className + "_0", cust.getName());
    assertEquals("correct group", 0, cust.getSocialGroup().getId());
    assertEquals("correct gender", "male", cust.getGender());
    assertEquals("correct car", "Tesla_40_kWh", cust.getCar().getName());
    cust = customers.get(2);
    assertEquals("correct name", className + "_2", cust.getName());
    assertEquals("correct group", 1, cust.getSocialGroup().getId());
    assertEquals("correct gender", "female", cust.getGender());
    assertEquals("correct car", "Tesla_40_kWh", cust.getCar().getName());
  }

  // Boot-restore works from a Configuration instance
  @Test
  public void testBootRestoreConfig ()
  {
    // need to configure manually for test
    serverConfiguration.addXmlConfiguration("test-properties.xml");
    Collection<?> escs =
        serverConfiguration.configureInstances(EvSocialClass.class);

    assertEquals("four classes", 4, escs.size());
    EvSocialClass target = null;
    Iterator<?> targets = escs.iterator();
    while (targets.hasNext() && null == target) {
      EvSocialClass candidate = (EvSocialClass)targets.next();
      if (candidate.getName().equals("HighIncome_2")) {
        target = candidate;
      }
    }

    assertNotNull("found target", target);
    assertNull("customer attribute list created",
        target.getCustomerAttributeList());

    target.setServiceAccessor(service);
    target.initialize();

    // make sure initialization does not mess it up
    assertNotNull("customer attribute list created",
                  target.getCustomerAttributeList());
    assertEquals("15 elements of customer attribute list", 15,
                 target.getCustomerAttributeList().size());

    List<EvCustomer> customers = target.getEvCustomers();
    assertEquals("15 customers", 15, customers.size());
    assertFalse("first customer is not driving", customers.get(0).isDriving());
  }

  class DummyConfig implements ServerConfiguration
  {
    private Configurator configurator;
    private CompositeConfiguration config;

    DummyConfig ()
    {
      super();
    }

    void initialize ()
    {
      config = new CompositeConfiguration();
      configurator = new Configurator();
      InputStream stream =
          ConfigTest.class.getResourceAsStream("/config/test-properties.xml");
      XMLConfiguration xconfig = new XMLConfiguration();
      try {
        xconfig.load(stream);
        config.addConfiguration(xconfig);
        configurator.setConfiguration(config);
      }
      catch (ConfigurationException e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    void addXmlConfiguration (String filename)
    {
      InputStream stream =
          ConfigTest.class.getResourceAsStream("/config/" + filename);
      XMLConfiguration xconfig = new XMLConfiguration();
      try {
        xconfig.load(stream);
        config.addConfiguration(xconfig);
      }
      catch (ConfigurationException e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    void addPropertiesConfiguration (String filename)
    {
      InputStream stream =
          ConfigTest.class.getResourceAsStream("/config/" + filename);
      PropertiesConfiguration xconfig = new PropertiesConfiguration();
      try {
        xconfig.load(stream);
        config.addConfiguration(xconfig);
      }
      catch (ConfigurationException e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    @Override
    public void configureMe (Object target)
    {
      configurator.configureSingleton(target);
    }

    @Override
    public Collection<?> configureInstances (Class<?> target)
    {
      return configurator.configureInstances(target);
    }

    @Override
    public void publishConfiguration (Object target)
    {
      
    }

    @Override
    public void saveBootstrapState (Object thing)
    {
      
    }

    @Override
    public Collection<?> configureNamedInstances (List<?> instances)
    {
      return configurator.configureNamedInstances(instances);
    }

    void printKeys ()
    {
      Iterator<String> keys = config.getKeys();
      while (keys.hasNext()) {
        String key = keys.next();
        System.out.println(key);
      }
    }
  }

  /**
   * Configuration recorder for publishing config info to brokers
   */
  class ConfigurationPublisher implements ConfigurationRecorder
  {
    Properties publishedConfig;
    
    ConfigurationPublisher ()
    {
      publishedConfig = new Properties();
    }

    @Override
    public void recordItem (String key, Object value)
    {
      publishedConfig.put(key, value);      
    }
    
    Properties getConfig ()
    {
      return publishedConfig;
    }
  }

  class ServiceAccessor implements CustomerServiceAccessor
  {

    @Override
    public CustomerRepo getCustomerRepo ()
    {
      return mockCustomerRepo;
    }

    @Override
    public RandomSeedRepo getRandomSeedRepo ()
    {
      return mockSeedRepo;
    }

    @Override
    public TariffRepo getTariffRepo ()
    {
      return tariffRepo;
    }

    @Override
    public TariffSubscriptionRepo getTariffSubscriptionRepo ()
    {
      return tariffSubscriptionRepo;
    }

    @Override
    public TimeslotRepo getTimeslotRepo ()
    {
      return timeslotRepo;
    }

    @Override
    public TimeService getTimeService ()
    {
      return null;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      return serverConfiguration;
    }
  }
}
