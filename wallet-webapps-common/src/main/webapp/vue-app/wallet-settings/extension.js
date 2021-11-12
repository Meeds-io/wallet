export function registerExternalComponents(componentName, component) {
  const externalComponentOptions = {
    name: componentName,
    componentImpl: component
  };

  if (extensionRegistry) {
    extensionRegistry.registerComponent('external-space', 'settings', externalComponentOptions);
  }
}