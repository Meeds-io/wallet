/*
 * Copyright (C) 2019 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.addon.wallet.blockchain;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * See {@link ExoBlockchainTransaction}
 */
@Aspect
public class ExoBlockchainTransactionAspect {
  private static final Log LOG = ExoLogger.getLogger(ExoBlockchainTransactionAspect.class);

  /**
   * Encapsulate method annotated with {@link ExoBlockchainTransaction} to
   * ensure that it's executed in Webapp ClassLoader where the bouncycastle is
   * defined with a newer version than platform, see PLF-8132
   * 
   * @param point processing point
   * @return result of processing point after its execution
   * @throws Throwable if processing point throws an exception or the webapp
   *           class loader isn't founds
   */
  @Around("execution(* *(..)) && @annotation(org.exoplatform.addon.wallet.blockchain.ExoBlockchainTransaction)")
  public Object around(ProceedingJoinPoint point) throws Throwable {
    ExoBlockchainTransactionService blockchainTransactionService = (ExoBlockchainTransactionService) point.getThis();
    if (blockchainTransactionService.getWebappClassLoader() == null) {
      throw new IllegalStateException("Webapp class loader is null");
    }
    ClassLoader currentThreadCL = startOperationInWebAppCL(blockchainTransactionService.getWebappClassLoader());
    try {
      return point.proceed();
    } catch (RuntimeException e) {
      LOG.error("Error while processing blockchain transactional method.", e);
      throw e;
    } finally {
      endOperationInWebappCL(currentThreadCL);
    }
  }

  private static final ClassLoader startOperationInWebAppCL(ClassLoader webappClassLoader) {
    Thread currentThread = Thread.currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(webappClassLoader);
    return currentClassLoader;
  }

  private static final void endOperationInWebappCL(ClassLoader currentClassLoader) {
    Thread currentThread = Thread.currentThread();
    currentThread.setContextClassLoader(currentClassLoader);
  }

}
