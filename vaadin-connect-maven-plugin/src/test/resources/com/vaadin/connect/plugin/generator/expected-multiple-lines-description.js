// Generated from GeneratorTestClass.java
import defaultClient from './connect-client.default';

/**
 * This class is used
 * <h1>for OpenApi generator test</h1>
 */
export class GeneratorTestClass {

  /**
   * Create a GeneratorTestClass instance.
   * @param {ConnectClient=} client - an instance of ConnectClient
   */
  constructor(client = defaultClient) {
    this._client = client;
  }

  /**
   * Get all users
   *
   * @returns {Promise<array>} Return list of users
   */
  getAllUsers() {
    return this._client.call('GeneratorTestClass', 'getAllUsers', undefined, {requireCredentials: false});
  }
}

const service = new GeneratorTestClass(defaultClient);


/**
 * Get all users
 *
 * @returns {Promise<array>} Return list of users
 */
export function getAllUsers() {
  return service.getAllUsers.apply(service, arguments);
}