
export function addSpaceSettings(name, componentImpl) {
  const externalComponentOptions = {
    appId: 'SpaceWallet',
    name: name,
    componentImpl: componentImpl
  };
  document.dispatchEvent(new CustomEvent('addSpaceSettingsExternalComponents', {'detail': externalComponentOptions}));
}

