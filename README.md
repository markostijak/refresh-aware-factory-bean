**This solution is based on Spring FactoryBean interface and the way how Spring container uses it.**

# refresh-aware-factory-bean #

Refresh Aware Factory Bean is a Spring Boot module to help with Java objects which content should be updated at some intervals from file or other sources. Implementation is based on the FactoryBean interface and AOP Framework provided by Spring.

The main idea is borrowed from Spring `AbstractFactoryBean<T>` and then extended with refresh support in form of `RefreshAwareFactoryBean<T>` as base class and `ProxyBasedRefreshAwareFactoryBean<T>` which should be used to create custom RefreshAwareFactoryBean-s.

---

**Problem**

Let's imagine that we have some job which creates/updates file content every hour. In that file there is data which we want to use in our application and once file content is updated, we want those changes reflected in our application.

To keep things simple, let's imagine that file content is an array of numbers (`numbers.txt`). In our application we could use this data in `List<Integers>` form. So the idea is to create some kind of List implementation that 'can' track file changes and update itself. The idea is not to update the content of the list, but to create a new list and then start using that new instance. This is useful when new data is corrupted and should not be used.

**Solution**

Split implementation into two parts. RefreshAwareFactoryBean which will provide a life-cycle for some instance and Dynamic Proxy which knows how to delegate all calls to 'real' instance currently held by that RefreshAwareFactoryBean. RefreshAwareFactoryBean life-cycle is described below in implementation details.

Users should only implement custom RefreshAwareFactoryBean by extending `ProxyRefreshAwareFactoryBean<T>` base class and dynamic proxy will be automatically created based on instance `T` type.

This approach can be used for any Java type (provided or newly created) which is not **final**!

For example:
- Both, List and ArrayList can be used with this solution.
- String class can't be used because it's final and can't be proxied, but CharSequence interface can.

Benefits from this approach are clean and unit testable code and simple usage. Disadvantages are usage of Dynamic Proxies and it's a little harder to understand the logic behind this solution.

**Example**

For given file (`numbers.txt`) content:

```
1, 2, 3, 4, 5, 6, 7, 8, 9
```
We just need to implement an appropriate RefreshAwareFactoryBean which will track file and 'update' List content. In this case we can extend `FileProxyRefreshAwareFactoryBean` to create a List of Integers from file. Here we are using `FileProxyRefreshAwareFactoryBean` because it knows how to check if file content is modified and when to call `refreshInstance` (delegated to `createInstance` by default) to create a new numbers List with new data.

```java
@Component("numbers")
public class NumbersFactoryBean extends FileProxyRefreshAwareFactoryBean<List<Integer>> {

   @Autowired
   public NumbersFactoryBean(@Value("${filepath-from-properties}") Path filepath) {
      super(filepath);
   }

   @NonNull
   @Override
   protected List<Integer> createInstance(@NonNull Path filepath) throws Exception {
      String content = Files.readString(filepath);
      String[] numbers = content.split(", *");
      
      return Stream.of(numbers).map(Integer::parseInt).collect(toList());
   }
   
}
```
and then in our NumberService we can autowire `numbers` list and use it:

```java
@Service
public class NumbersService {

   private final List<Integer> numbers;

   @Autowired
   public NumbersService(@Qualifier("numbers") List<Integer> numbers) {
      this.numbers = numbers;
   }

   public void doSomething() {
     // use numbers here...
   }
 
}
```
---

# Implementation details

RefreshAwareFactoryBean implements the `RefresableBean` interface in order to be notified for refresh events. `RefresableBean` interface provides a single `refresh` method which is called by different thread managed by Spring TaskScheduler at intervals specified in application properties. You must use `@EnableScheduling` annotation in order to enable beans refreshing. This is implemented using Spring auto configuration approach, so all you need to do is include this module in your project and start using it.

This are all properties that you can configure:
- `enabled` - Enable beans refreshing. Default is `true`.
- `initial-delay` - Delay before the first refresh. Default is 1 minute. Ignored in case of cron usage.
- `fixed-rate` - Call refresh with a fixed period between invocations. Default is 1 minute.
- `fixed-delay` - Call refresh with a fixed period between the end of the last invocation and the start of the next. If specified, it has priority over `fixedRate` property.
- `cron` - Call refresh with specified cron. If specified, it has priority over `fixed-delay` and `fixed-rate` properties.

**Lifecycle**

RefreshAwareFactoryBean lifecycle is managed by five methods: `afterPropertiesSet`, `getObjectType`, `getObjectType`, `refresh` and `destroy`. These methods are all called by Spring Container. Beside these five methods, there are five template methods `createInstance`, `createDummyInstance`, `shouldRefresh`, `refreshInstance`, `destroyInstance` which end user has to override (some are optional) in order to implement specific RefreshAwareFactoryBean behaviour.

On application startup, Spring IOC Container will create RefreshAwareFactoryBean autowiring all required dependencies. After this, `afterPropertiesSet` method is called. This method will first try to call `createInstance` in order to create an initial instance to be autowired where it is required. If `createInstance` for some reason fails (exception occurred), then `createDummyInstance` is called. By default `createDummyInstance` will return `null` and this means that application is in an illegal state because there is no initial instance that can be autowired and application won't start (`IllegalStateException` will be thrown). But, if you want an application to continue to work, you can provide some dummy instance by overriding `createDummyInstance` (e.g. empty list) and that instance will be used as an initial instance. This is useful when creating instance from file content but the file is missing or is corrupted. Also, in this step instance type resolution is performed because it is needed for Dynamic Proxy which is also created.

(Once all `RefreshableBean`s are created, they are collected into List and autowired to `RefreshableBeanAutoConfiguration` for refresh scheduling.)

Now that RefreshAwareFactoryBean is initialized, Spring IOC Container will detect that the created bean is of FactoryBean type which means it needs to call `getObjectType` method in order to receive information about the object type this factory creates. With this information Spring IOC knows which beans depend on instance kept by RefreshAwareFactoryBean. By calling getObject, Spring IOC will obtain **proxy** created by this factory bean. In every moment, **proxy** knows how to delegate method calls to "real" instance kept by this RefreshAwareFactoryBean.

After `initial-delay`, first refresh is performed by calling the `refresh` method from RefreshAwareFactoryBean (`refresh` is called by scheduled thread in `RefreshableBeanAutoConfiguration` of this module). Inside this method, the first is called the `shouldRefresh` method. It should signal the new instance should be created (e.g. file content is updated). In case `shouldRefresh` returns `true`, `refreshInstance` is called. By default, `refreshInstance` will call `createInstance`. If you need some custom refresh logic, you can override `refreshInstance`. After a successful refresh, a new instance is created, and the current instance is replaced with a newly created instance. After that, `destroyInstance` is called with an old instance as an argument in order to close opened resources. All calls through Dynamic Proxy now delegate method calls to newly created instance. This process is repeated for every refresh event.
Note that you can pass TaskScheduler as a constructor parameter if you want `refreshInstance` and `destroyInstance` to be called in seperate threads with configurable delays (e.g. wait 10 seconds to make sure old instance is not used by anyone). Also, once an old instance is replaced with a new one, the switch is instant in all parts of the application.

At the end, on application shutdown, Spring IOC Container will call `destroy` method. In this case `destroyInstance` will be called with the currently held instance by RefreshAwareFactoryBean.

**API**

- `afterPropertiesSet` - Called at application startup. Calls `createInstance` and `createDummyInstance`.
- `createInstance` - Creates instance.
- `createDummyInstance` - Creates a dummy instance to be used in case when createInstance fails. This is optional.
- `refresh` - Called at predefined intervals. Default is 1 minute and it can be set in application properties. Calls `shouldRefresh`, `refreshInstance` and `destroyInstance`. Supports async refresh by providing `TaskScheduler` as constructor parameter.
- `shouldRefresh` - Signals when the instance should be refreshed (recreated).
- `refreshInstance` - Recreates instance based on new updated content. Default implementation will call `createInstance`.
- `destroyInstance` - Destroys previously created instance. Default implementation will try to call `close` from `AutoCloseable`.
- `destroy` - Called at application shutdown. Calls `destroyInstance` with the currently held instance.
- `getObject` - Return an instance of the object managed by this factory.
- `getObjectType` - Return the type of object that this FactoryBean creates.

**Provided implementations**

There are several specific implementations of RefreshAwareFactoryBean:
- `ProxyRefreshAwareFactoryBean<T>` - Generic base class for others to extend from. Specific implementation is required to override **createInstance** and **shouldRefresh** methods in order to create java objects and to signal when it should be updated (recreated) respectively. Methods *refreshInstance* and *destroyInstance* are optional to override. By default *refreshInstance* will call **createInstance** and *destroyInstance* method will call *close* if the object implements AutoCloseable interface.
- `FileProxyRefreshAwareFactoryBean<T>` - Creates Java object based on file content and recreates it once the file content is updated, knows when file content is updated. Specific implementation is required to override **createInstance** method in order to parse file content into Java objects.
- `JsonFileProxyRefreshAwareFactoryBean<T>` - Similar as previous, but knows how to parse a json file content
- `YamlFileProxyRefreshAwareFactoryBean<T>` - Similar as previous, but knows how to parse a yaml file content

# Additional informations

**Java CDI**

This implementation is specific to Spring Framework. If you need Java CDI implementation of this, you can contact me for more information. I have it implemented and it uses a slightly different approach and requires one additional `@Producer` method in every RefreshAwareFactoryBean.

**Performances**

Application performances can depend on the number of used dynamic proxies in it. If you want to improve performances of Dynamic Proxies created by this module, you could switch to ByteBuddy and you will be able to create Dynamic Proxies with a direct call to method, eliminating any additional performance overhead. Contact me for more information.
