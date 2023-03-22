/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.lock;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class LockAspect {

  private static final Map<String, StampedLock> LOCKS = new HashMap<>();

  @Around("execution(* *(..)) && @annotation(io.meeds.wallet.lock.Lock)")
  public Object around(ProceedingJoinPoint point) throws Throwable { // NOSONAR
    MethodSignature methodSignature = (MethodSignature) point.getSignature();
    Method method = methodSignature.getMethod();
    Lock annotation = method.getAnnotation(Lock.class);

    String id = annotation.id();
    if (StringUtils.isBlank(id)) {
      id = method.toString();
    }
    StampedLock lock = LOCKS.computeIfAbsent(id, key -> new StampedLock());
    long stamp = lock.tryWriteLock(annotation.duration(), annotation.timeUnit());
    try {
      return point.proceed();
    } finally {
      lock.unlock(stamp);
    }
  }

}
