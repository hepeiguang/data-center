window.APP_CONFIG = {
  singleSignOnValue: false,
  showMenu: true,
  ssoConfig: {
    mode: 'oauth', // 空则不使用单点  支持的模式有  cas oauth basic
    // mode: 'jwt', // 空则不使用单点  支持的模式有  cas oauth basic
    // url: 'http://172.17.92.169:11629/security/oauth/authorize',
    // clientId: 'SJPT', // oauth模式下需要提供
    // logout: 'http://172.17.92.169:11629/security/signIn.html',
    url: 'http://172.30.30.1:15200/security/oauth/authorize',
    clientId: 'SJPT', // oauth模式下需要提供
    logout: 'http://172.30.30.1:15200/security/signIn.html',
    redirectUrl: 'http://43.136.32.15:8024/dolphinscheduler/ui/' // oauth模式下需要提供
  }
}