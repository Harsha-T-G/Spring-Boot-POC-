requestInterceptor: async function(request) {
  const target = new URL(request.url, window.location.href);
  if (target.origin !== window.location.origin) {
    throw new Error('Only same-origin API requests are allowed');
  }
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes((request.method || 'GET').toUpperCase())) {
    const base = window.location.pathname.split('/swagger-ui')[0];
    const response = await fetch(base + '/api/csrf', {credentials: 'same-origin', cache: 'no-store'});
    if (!response.ok) {
      throw new Error('Authentication required; reopen Swagger and use the browser credential prompt');
    }
    const csrf = await response.json();
    request.headers = request.headers || {};
    request.headers[csrf.headerName] = csrf.token;
  }
  return request;
},
