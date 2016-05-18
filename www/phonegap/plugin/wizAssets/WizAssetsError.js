/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Wizcorp Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
*/

/**
 * WizAssetsError
 */
function WizAssetsError(error) {
  this.code = error || null;
}

// WizAssets error codes
WizAssetsError.ARGS_MISSING_ERROR = 1;
WizAssetsError.INVALID_URL_ERROR = 2;
WizAssetsError.CONNECTIVITY_ERROR = 3;
WizAssetsError.HTTP_REQUEST_ERROR = 4;
WizAssetsError.HTTP_REQUEST_CONTENT_ERROR = 5;
WizAssetsError.DIRECTORY_CREATION_ERROR = 6;
WizAssetsError.FILE_CREATION_ERROR = 7;
WizAssetsError.JSON_CREATION_ERROR = 8;

module.exports = WizAssetsError;