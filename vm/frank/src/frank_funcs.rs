/*
 * Copyright 2019 Fluence Labs Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use crate::config::Config;
use crate::errors::InstantiationError;
use wasmer_runtime::{Func, Instance};

// the rental macro requires fields to have only one type parameter
pub struct FrankFuncs<'a> {
    pub allocate: Func<'a, i32, i32>,
    pub deallocate: Func<'a, (i32, i32), ()>,
    pub invoke: Func<'a, (i32, i32), i32>,
}

impl<'a> FrankFuncs<'a> {
    pub fn new(instance: &'a Instance, config: &'a Config) -> Result<Self, InstantiationError> {
        let allocate = instance.func::<(i32), i32>(&config.allocate_function_name)?;
        let deallocate = instance.func::<(i32, i32), ()>(&config.deallocate_function_name)?;
        let invoke = instance.func::<(i32, i32), i32>(&config.invoke_function_name)?;

        Ok(Self {
            allocate,
            deallocate,
            invoke,
        })
    }
}
