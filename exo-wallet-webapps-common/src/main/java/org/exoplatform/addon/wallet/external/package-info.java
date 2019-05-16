/**
 * This package is added to add a workaround for the bouncycastle dependency
 * conflict with pre-bundeled version in eXo Platform 5.2.
 * 
 * Once the conflict is resolved, the classes: GlobalSettingsModificationListener,
 * ServiceLoaderServlet and EthereumClientConnectorForTransaction aren't usefull
 * anymore.
 * 
 * The class EthereumWalletTokenAdminService should be moved to
 * wallet-services artifact with a configuration to instantiate the service.
 * 
 * See PLF-8123
 */
package org.exoplatform.addon.wallet.external;
