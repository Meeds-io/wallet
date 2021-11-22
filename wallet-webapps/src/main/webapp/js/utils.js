
export function addSpaceSettings(name, componentImpl) {
  const externalComponentOptions = {
    name: name,
    componentImpl: componentImpl
  };
  document.dispatchEvent(new CustomEvent('addSpaceSettingsExternalComponents', {'detail': externalComponentOptions}));
}

