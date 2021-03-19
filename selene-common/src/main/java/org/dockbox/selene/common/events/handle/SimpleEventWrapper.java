/*
 * Copyright (C) 2020 Guus Lieben
 *
 * This framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.selene.common.events.handle;

import org.dockbox.selene.api.ThreadUtils;
import org.dockbox.selene.api.annotations.Async;
import org.dockbox.selene.api.annotations.event.IsCancelled;
import org.dockbox.selene.api.annotations.event.filter.Filter;
import org.dockbox.selene.api.annotations.event.filter.Filters;
import org.dockbox.selene.api.events.EventBus;
import org.dockbox.selene.api.events.EventStage;
import org.dockbox.selene.api.events.EventWrapper;
import org.dockbox.selene.api.events.parents.Cancellable;
import org.dockbox.selene.api.events.parents.Event;
import org.dockbox.selene.api.events.parents.Filterable;
import org.dockbox.selene.api.events.processing.AbstractEventParamProcessor;
import org.dockbox.selene.api.exceptions.SkipEventException;
import org.dockbox.selene.api.server.Selene;
import org.dockbox.selene.api.util.Reflect;
import org.dockbox.selene.api.util.SeleneUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper type for future invokation of a {@link Method} listening for {@link Event} posting. This
 * type is responsible for filtering and invoking a {@link Method} when a supported {@link Event} is
 * fired.
 */
public final class SimpleEventWrapper implements Comparable<SimpleEventWrapper>, EventWrapper {
  public static final Comparator<SimpleEventWrapper> COMPARATOR =
      (o1, o2) -> {
        if (fastEqual(o1, o2)) return 0;

        // @formatter:off
        int c;
        if (0 != (c = Integer.compare(o1.priority, o2.priority))) return c;
        if (0 != (c = o1.method.getName().compareTo(o2.method.getName()))) return c;
        if (0 != (c = o1.eventType.getName().compareTo(o2.eventType.getName()))) return c;
        if (0 != (c = Integer.compare(o1.listener.hashCode(), o2.listener.hashCode()))) return c;
        if (0 != (c = Integer.compare(o1.hashCode(), o2.hashCode()))) return c;
        // @formatter:on
        throw new AssertionError(); // ensures the comparator will never return 0 if the two wrapper
        // aren't equal
      };
  private final Object listener;
  private final Class<? extends Event> eventType;
  private final Method method;
  private final int priority;

  private SimpleEventWrapper(
      Object listener, Class<? extends Event> eventType, Method method, int priority) {
    this.listener = listener;
    this.eventType = eventType;
    this.method = method;
    this.priority = priority;
  }

  /**
   * Creates one or more {@link SimpleEventWrapper}s (depending on how many event parameters are
   * present) for a given method and instance.
   *
   * @param instance The instance which is used when invoking the method.
   * @param method The method to store for invokation.
   * @param priority The priority at which the event is fired.
   * @return The list of {@link SimpleEventWrapper}s
   */
  public static List<SimpleEventWrapper> create(Object instance, Method method, int priority) {
    List<SimpleEventWrapper> invokeWrappers = SeleneUtils.emptyConcurrentList();
    for (Class<?> param : method.getParameterTypes()) {
      if (Reflect.isAssignableFrom(Event.class, param)) {
        @SuppressWarnings("unchecked")
        Class<? extends Event> eventType = (Class<? extends Event>) param;
        invokeWrappers.add(new SimpleEventWrapper(instance, eventType, method, priority));
      }
    }
    return invokeWrappers;
  }

  @Override
  public void invoke(Event event) throws SecurityException {
    try {
      Collection<Object> args = this.getEventArgs(event);

      // Listener methods may be private or protected, before invoking it we need to ensure it is
      // accessible.
      if (!this.method.isAccessible()) {
        this.method.setAccessible(true);
      }

      if (this.filtersMatch(event) && this.acceptsState(event)) {
        Runnable eventRunner =
            () -> {
              try {
                this.method.invoke(this.listener, args.toArray());
              } catch (Throwable e) {
                /*
                Typically this is caused by a exception thrown inside the event itself. It is possible that
                the arguments provided to Method#invoke are incorrect, depending on external annotation
                processors.
                */
                Selene.handle("Could not finish event runner", e);
              }
            };

        ThreadUtils tu = Selene.provide(ThreadUtils.class);
        if (this.method.isAnnotationPresent(Async.class)) {
          tu.performAsync(eventRunner);
        } else {
          eventRunner.run();
        }
      }
    } catch (SkipEventException ignored) {
      /*
      SkipEventException can be thrown by (a) AbstractEventParamProcessor(s), indicating the method should
      not be invoked. Usually this is because of a filter application of the processor.
      */
    }
  }

  @NotNull
  private Collection<Object> getEventArgs(Event event) throws SkipEventException {
    EventBus bus = Selene.provide(EventBus.class);

    Collection<Object> args = SeleneUtils.emptyList();
    for (Parameter parameter : this.method.getParameters()) {
      /*
      Arguments always default to null if it is not assignable from the event type provided, and should be
      populated by annotation processors.
      */
      Object argument = null;
      if (Reflect.isAssignableFrom(parameter.getType(), event.getClass())) argument = event;

      /*
      To allow for the addition of future stages, we only use the enum values provided directly. This way we can
      avoid having to modify this type if future stages are added to EventStage.
      */
      for (EventStage stage : EventStage.values()) {
        argument = this.processObjectForStage(argument, parameter, event, stage, bus);
      }
      args.add(argument);
    }

    return args;
  }

  private boolean filtersMatch(Event event) {
    /*
    If a event is Filterable and has one or more Filter annotations, we test for these filters to decide whether
    or not we can invoke this method. These filters act on the given filter and event, and unlike paramater
    annotation processors do not have access to the InvokeWrapper, Method or listener objects.
    */
    if (event instanceof Filterable) {
      if (this.method.isAnnotationPresent(Filter.class)) {
        Filter filter = this.method.getAnnotation(Filter.class);
        return SimpleEventWrapper.testFilter(filter, (Filterable) event);

      } else if (this.method.isAnnotationPresent(Filters.class)) {
        Filters filters = this.method.getAnnotation(Filters.class);
        for (Filter filter : filters.value()) {
          if (!SimpleEventWrapper.testFilter(filter, (Filterable) event)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean acceptsState(Event event) {
    /*
    If a event can be cancelled, listeners can indicate their preference on whether or not they wish to listen for
    events which are cancelled or non-cancelled, or either. If the event cannot be cancelled this always returns
    true.
    */
    if (event instanceof Cancellable) {
      Cancellable cancellable = (Cancellable) event;
      if (this.method.isAnnotationPresent(IsCancelled.class)) {
        switch (this.method.getAnnotation(IsCancelled.class).value()) {
          case TRUE:
            return cancellable.isCancelled();
          case FALSE: // Default behavior
            return !cancellable.isCancelled();
          case UNDEFINED: // Either is accepted
            return true;
        }
      } else return !cancellable.isCancelled();
    }
    return true;
  }

  private Object processObjectForStage(
      @Nullable Object argument, Parameter parameter, Event event, EventStage stage, EventBus bus)
      throws SkipEventException {
    for (Annotation annotation : parameter.getAnnotations()) {
      /*
      A annotation may be decorative or provide meta-data, rather than be a processor indicator. If no processor
      is available continue looking up the next annotation (if any).
      */
      @SuppressWarnings("unchecked")
      AbstractEventParamProcessor<Annotation> processor =
          bus.getParamProcessor((Class<Annotation>) annotation.getClass(), stage);
      if (null == processor) continue;

      /*
      Ensure we are in the expected stage for the processor, as different processors may wish to act on different
      stages of the event construction for the same parameter annotation.
      */
      EventStage targetStage = processor.targetStage();
      if (targetStage != stage) continue;
      argument = processor.process(argument, annotation, event, parameter, this);
    }
    return argument;
  }

  private static boolean testFilter(Filter filter, Filterable event) {
    if (event.acceptedParams().contains(filter.param())
        && event.acceptedFilters().contains(filter.type())) {
      return event.isApplicable(filter);
    }
    return false;
  }

  @Override
  public Object getListener() {
    return this.listener;
  }

  @Override
  public Class<? extends Event> getEventType() {
    return this.eventType;
  }

  @Override
  public Method getMethod() {
    return this.method;
  }

  @Override
  public int getPriority() {
    return this.priority;
  }

  @Override
  public int compareTo(@NotNull SimpleEventWrapper o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public int hashCode() {
    int n = 1;
    n = 31 * n + this.listener.hashCode();
    n = 31 * n + this.eventType.hashCode();
    n = 31 * n + this.method.hashCode();
    return n;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SimpleEventWrapper)) return false;
    return fastEqual(this, (SimpleEventWrapper) o);
  }

  private static boolean fastEqual(SimpleEventWrapper o1, SimpleEventWrapper o2) {
    return Objects.equals(o1.listener, o2.listener)
        && Objects.equals(o1.eventType, o2.eventType)
        && Objects.equals(o1.method, o2.method);
  }

  @Override
  public String toString() {
    return String.format(
        "InvokeWrapper{listener=%s, eventType=%s, method=%s(%s), priority=%d}",
        this.listener,
        this.eventType.getName(),
        this.method.getName(),
        this.eventType.getSimpleName(),
        this.priority);
  }
}
